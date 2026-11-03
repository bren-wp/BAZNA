package com.brendigo.sidrena.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Single validated reader for the official public WordPress discovery contract.
 * No catalogue or legal profile is accepted from an unsupported or ambiguous API.
 */
object CatalogDiscovery {
    private const val MAX_OUTLETS = 500
    private val codePattern = Regex("[A-Za-z0-9][A-Za-z0-9-]{0,63}")

    fun parse(json: JSONObject, site: SiteUrl): CatalogIndex {
        require(json.optInt("api_version", -1) == 1) {
            "Ova verzija javnog SIDRENA API-ja nije podržana."
        }
        // Optional for older plugins, mandatory to validate when supplied:
        // Android can display public prices but must never negotiate an admin
        // login or a write-capable API from an unexpected discovery response.
        when (val connection = json.opt("connection")) {
            null, JSONObject.NULL -> Unit
            is JSONObject -> {
                require(connection.optString("mode") == "public_read_only") {
                    "SIDRENA dopušta samo javno čitanje cjenika."
                }
                require(connection.opt("authentication_required") == false) {
                    "SIDRENA ne podržava prijavu u WordPress administraciju."
                }
                require(connection.opt("csv_publication_required") == true) {
                    "Poslužitelj ne potvrđuje sigurnu objavu CSV cjenika."
                }
                val endpoint = connection.optString("rest_endpoint")
                if (endpoint.isNotBlank()) site.approved(endpoint)
            }
            else -> throw IllegalArgumentException("Nevaljan opis WordPress povezivanja.")
        }

        val sources = json.optJSONObject("feeds")
            ?: throw IllegalArgumentException("Nedostaju izvori službenih cjenika.")

        fun csvFrom(feeds: JSONObject, kind: String): String {
            val source = feeds.optJSONObject(kind)
                ?: throw IllegalArgumentException("Nedostaje izvor za $kind.")
            val csv = source.optString("csv").trim()
            require(csv.isNotEmpty()) { "Nedostaje CSV poveznica za $kind." }
            return site.approved(csv).toASCIIString()
        }

        // Optional v1 field introduced by the WordPress plugin. Missing means
        // unknown (older plugin), and must never be interpreted as not ready.
        fun ready(feeds: JSONObject, kind: String): Boolean? {
            val source = feeds.optJSONObject(kind)
                ?: throw IllegalArgumentException("Nedostaje izvor za $kind.")
            return when (val raw = source.opt("available_csv")) {
                null, JSONObject.NULL -> null
                is Boolean -> raw
                else -> throw IllegalArgumentException("Status CSV cjenika nije valjan.")
            }
        }

        val seen = HashSet<String>()
        val outlets = mutableListOf<Outlet>()
        val rawOutlets = when (val field = json.opt("outlets")) {
            null, JSONObject.NULL -> JSONArray()
            is JSONArray -> field
            else -> throw IllegalArgumentException("Popis poslovnica nije valjan.")
        }
        require(rawOutlets.length() <= MAX_OUTLETS) {
            "Poslužitelj je vratio prevelik broj poslovnica."
        }
        for (i in 0 until rawOutlets.length()) {
            val row = rawOutlets.optJSONObject(i)
                ?: throw IllegalArgumentException("Podaci o poslovnici nisu valjani.")
            val code = row.optString("code")
            require(codePattern.matches(code) && seen.add(code.lowercase())) {
                "Oznaka poslovnice nije valjana ili je ponovljena."
            }
            val name = row.optString("name")
            require(name.isNotBlank() && name.length <= 240) {
                "Naziv poslovnice nije valjan."
            }
            val feed = row.optJSONObject("feeds")
                ?: throw IllegalArgumentException("Poslovnica nema izvore cjenika.")
            outlets.add(
                Outlet(
                    name = name,
                    code = code,
                    productCsv = csvFrom(feed, "proizvodi"),
                    serviceCsv = csvFrom(feed, "usluge"),
                    productsCsvReady = ready(feed, "proizvodi"),
                    servicesCsvReady = ready(feed, "usluge"),
                ),
            )
        }

        fun publicPage(key: String): String? {
            val url = json.optString(key).trim()
            return if (url.isEmpty()) null else site.approved(url).toASCIIString()
        }
        val rawLegal = json.opt("legal")
        val legal = when (rawLegal) {
            null, JSONObject.NULL -> null
            is JSONObject -> LegalContract.parseOptional(rawLegal)
            else -> throw IllegalArgumentException("Pravni profil nije valjan.")
        }
        val archiveApi = json.optString("type_archive")
        if (archiveApi.isNotBlank()) site.approved(archiveApi)

        val publication = json.optJSONObject("publication")
        val pending = when (val flag = publication?.opt("pending_changes")) {
            null, JSONObject.NULL -> null
            is Boolean -> flag
            else -> throw IllegalArgumentException("Nevaljan status neobjavljenih promjena.")
        }
        val certified = publication?.opt("compliance_certified")
        require(certified == null || certified == JSONObject.NULL ||
            (certified is Boolean && !certified)
        ) { "Status pravne usklađenosti nije pouzdan." }

        return CatalogIndex(
            publishedAt = json.optString("published_at").ifBlank { null },
            publicPage = publicPage("cjenik"),
            archivePage = publicPage("archive"),
            productsCsv = csvFrom(sources, "proizvodi"),
            servicesCsv = csvFrom(sources, "usluge"),
            outlets = outlets,
            pluginVersion = json.optString("plugin_version").ifBlank { null },
            legal = legal,
            productsCsvReady = ready(sources, "proizvodi"),
            servicesCsvReady = ready(sources, "usluge"),
            pendingPublication = pending,
        )
    }
}
