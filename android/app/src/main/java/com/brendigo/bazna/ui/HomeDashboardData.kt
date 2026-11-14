package com.brendigo.bazna.ui

import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.SiteUrl
import java.net.URI
import java.text.NumberFormat
import java.util.Locale

internal data class HomeMetric(val label: String, val count: String, val route: String)
internal data class HomePublication(
    val title: String,
    val format: String,
    val date: String?,
    val url: String,
)

/** Counts the two actual public CSV feeds; never uses the reference's demo numbers. */
internal fun homeMetrics(
    data: CatalogSnapshot,
    selectedOutlet: String = "",
): List<HomeMetric> {
    val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("hr-HR"))
    val site = runCatching { SiteUrl.parse(data.site) }.getOrNull()
    val outlet = data.index.outlets.firstOrNull { it.code == selectedOutlet }
    val currentFeeds = if (selectedOutlet.isNotBlank() && outlet == null) {
        emptyList()
    } else {
        listOf(
            outlet?.productCsv ?: data.index.productsCsv,
            outlet?.serviceCsv ?: data.index.servicesCsv,
        )
    }
    val actualCsvCount = currentFeeds.mapNotNull { site?.publicLink(it) }.distinct().size
    val actualArchiveCount = publishedArchiveEntries(data).size
    return listOf(
        HomeMetric("Proizvodi", number.format(data.products.size), "products"),
        HomeMetric("Usluge", number.format(data.services.size), "services"),
        HomeMetric("Cjenici", number.format(actualCsvCount), "cjenici"),
        HomeMetric("Arhiva", number.format(actualArchiveCount), "archive"),
    )
}

/** A connected site can have CSV files pending publication; do not show green for those. */
internal fun homeCatalogPending(data: CatalogSnapshot, selectedOutlet: String = ""): Boolean {
    val outlet = if (selectedOutlet.isBlank()) null else {
        data.index.outlets.firstOrNull { it.code == selectedOutlet }
    }
    if (selectedOutlet.isNotBlank() && outlet == null) return true
    if (data.index.pendingPublication == true) return true
    val productReady = if (outlet == null) data.index.productsCsvReady else outlet.productsCsvReady
    val serviceReady = if (outlet == null) data.index.servicesCsvReady else outlet.servicesCsvReady
    return productReady == false || serviceReady == false
}

/**
 * Three latest PUBLIC docs, not imaginary activity data. Archive dates are preferred
 * where newer, and current CSV feeds can also appear as actual publications.
 */
internal fun homeRecentPublications(
    data: CatalogSnapshot,
    selectedOutlet: String = "",
): List<HomePublication> {
    val archive = publishedArchiveEntries(data).map {
        HomePublication(
            title = homeArchiveLabel(it),
            format = it.format.uppercase(Locale.ROOT),
            date = it.date.takeIf(String::isNotBlank),
            url = it.url,
        )
    }
    val current = publicPriceListDocuments(data, selectedOutlet)
        .asSequence()
        .filter { !it.archived && it.format == "CSV" }
        .map {
            HomePublication(
                title = it.title,
                format = it.format,
                date = it.date,
                url = it.url,
            )
        }.toList()
    return (current + archive)
        .distinctBy { it.url }
        .sortedByDescending { homeDateKey(it.date.orEmpty()) }
        .take(3)
}

/** Host is explanatory text only, never a rendered user-controlled hyperlink. */
internal fun homeSiteLabel(site: String): String = runCatching {
    val host = URI(site).host?.takeIf(String::isNotBlank)
    host ?: site.removePrefix("https://").trimEnd('/')
}.getOrDefault(site.removePrefix("https://").trimEnd('/'))

