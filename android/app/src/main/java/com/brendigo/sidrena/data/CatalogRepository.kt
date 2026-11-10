package com.brendigo.sidrena.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Uses only approved same-origin public BAZNA v1 endpoints. No password, cookies or
 * unpublished WordPress/WooCommerce records are accessed.
 */
class CatalogRepository {
    suspend fun load(inputSite: String, selectedOutlet: String = ""): CatalogSnapshot = withContext(Dispatchers.IO) {
        val site = SiteUrl.parse(inputSite)
        // Official WordPress REST supports both pretty and plain permalinks.
        // Do not retry on 401/403, TLS errors or an unavailable network.
        val indexJson = readDiscovery(site)
        val index = CatalogDiscovery.parse(indexJson, site)
        val selected = index.outlets.firstOrNull { it.code == selectedOutlet }
        require(selectedOutlet.isEmpty() || selected != null) { "Odabrana poslovnica više ne postoji." }
        val productUrl = selected?.productCsv ?: index.productsCsv
        val serviceUrl = selected?.serviceCsv ?: index.servicesCsv

        val warnings = mutableListOf<String>()
        if (index.wordpressLicenseActive == false) {
            warnings.add("BAZNA licenca za uređivanje nije aktivna. Već objavljeni cjenici i arhiva ostaju javno dostupni.")
        }
        if (index.pendingPublication == true) {
            warnings.add("Povezana BAZNA platforma ima neobjavljene promjene. Cijene u javnom CSV-u mogu biti zastarjele.")
        }
        if (index.legal == null) warnings.add("Povezana BAZNA platforma ne objavljuje pravni profil.")
        fun safeItems(url: String, label: String, ready: Boolean?): List<PriceItem> {
            if (ready == false) {
                warnings.add("Cjenik za " + label + " još nije javno objavljen. Objavite ga u administraciji BAZNA.")
                return emptyList()
            }
            if (url.isBlank()) {
                warnings.add("Nema objavljenog CSV cjenika: " + label + ".")
                return emptyList()
            }
            return try {
                CsvParser.items(read(site, site.approved(url), 6 * 1024 * 1024))
            } catch (exception: Exception) {
                warnings.add("Nije moguće učitati " + label + ". Provjerite objavu CSV cjenika.")
                emptyList()
            }
        }

        val products = safeItems(
            productUrl,
            "proizvode",
            if (selected == null) index.productsCsvReady else selected.productsCsvReady,
        )
        val services = safeItems(
            serviceUrl,
            "usluge",
            if (selected == null) index.servicesCsvReady else selected.servicesCsvReady,
        )
        val archive = try {
            val discoveryUrl = indexJson.optString("type_archive")
            val remote = if (discoveryUrl.isBlank())
                site.endpoint("wp-json/brendigo-sidrena/v1/arhiva")
            else site.approved(discoveryUrl)
            val list = JSONArray(read(site, remote, 512 * 1024))
            buildList {
                for (i in 0 until minOf(list.length(), 1000)) {
                    val item = list.optJSONObject(i) ?: continue
                    val outlet = item.optString("outlet")
                    if (selectedOutlet.isNotEmpty() && outlet != selectedOutlet) continue
                    if (selectedOutlet.isEmpty() && outlet.isNotBlank() && outlet != "webshop") continue
                    val url = site.publicLink(item.optString("url")) ?: continue
                    val format = item.optString("format")
                    if (format !in setOf("csv", "xml")) continue
                    add(ArchiveEntry(item.optString("date"), item.optString("kind"), outlet, format, url))
                }
            }
        } catch (exception: Exception) {
            warnings.add("Arhiva trenutačno nije dostupna.")
            emptyList()
        }

        CatalogSnapshot(site.display, index, products, services, archive, warnings)
    }

    private fun readDiscovery(site: SiteUrl): JSONObject {
        // All endpoints are same-origin HTTPS. Retry only route-not-found
        // (404/405) or a non-JSON CMS page; NEVER retry authentication,
        // TLS/DNS failures, timeouts or server errors on another platform.
        // Original WordPress pretty/plain aliases remain first.
        val candidates = platformDiscoveryCandidates(site)
        var last: Exception? = null
        for ((index, url) in candidates.withIndex()) {
            try {
                return JSONObject(read(site, url, 256 * 1024))
            } catch (exception: Exception) {
                if (index == candidates.lastIndex || !shouldFallbackToPlainRest(exception)) throw exception
                last = exception
            }
        }
        throw last ?: IOException("BAZNA API nije dostupan.")
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .proxy(Proxy.NO_PROXY)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                require(addresses.isNotEmpty() && addresses.all(PublicNetworkGuard::isPublic)) {
                    "Povezana stranica mora imati isključivo javne mrežne adrese."
                }
                return addresses
            }
        })
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun read(site: SiteUrl, initial: URI, maxBytes: Int): String {
        var url = site.approved(initial.toASCIIString())
        repeat(4) { redirect ->
            val request = Request.Builder()
                .url(url.toASCIIString())
                .get()
                .header("Accept", "application/json, text/csv, */*")
                // A public price list should be revalidated after publication.
                .header("Cache-Control", "no-cache, max-age=0")
                .header("User-Agent", "BAZNA-Android")
                .build()
            client.newCall(request).execute().use { response ->
                val code = response.code
                if (code in 300..399) {
                    if (redirect >= 3) throw IOException("Previše preusmjeravanja.")
                    val destination = response.header("Location")
                        ?: throw IOException("Preusmjeravanje nema odredište.")
                    url = site.approved(url.resolve(destination).toASCIIString())
                } else {
                    // Do not interpret a 204 or partial 206 response as a
                    // complete price list; incomplete exports are unsafe.
                    if (code != 200) throw IOException("HTTP " + code)
                    val output = ByteArrayOutputStream()
                    val body = response.body ?: throw IOException("Nedostaje sadržaj cjenika.")
                    body.byteStream().use { stream ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            if (output.size() + count > maxBytes) {
                                throw IOException("Datoteka prelazi sigurnosno ograničenje.")
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                    return output.toString(StandardCharsets.UTF_8.name())
                }
            }
        }
        throw IOException("Previše preusmjeravanja.")
    }
}

/** Only routing mismatches warrant an alternative WordPress endpoint. */
internal fun shouldFallbackToPlainRest(exception: Exception): Boolean =
    exception is JSONException ||
        (exception is IOException && exception.message in setOf("HTTP 404", "HTTP 405"))
