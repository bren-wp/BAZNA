package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.sidrena.R
import com.brendigo.sidrena.data.ArchiveEntry
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.SiteUrl
import java.net.URI
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/** Based on reference screens_concept/03_pocetna.png (1080x1920). */
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

/**
 * Latest PUBLIC docs, not imaginary activity data. Archive dates are preferred
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
        .take(2)
}

/** Host is explanatory text only, never a rendered user-controlled hyperlink. */
internal fun homeSiteLabel(site: String): String = runCatching {
    val host = URI(site).host?.takeIf(String::isNotBlank)
    host ?: site.removePrefix("https://").trimEnd('/')
}.getOrDefault(site.removePrefix("https://").trimEnd('/'))

/**
 * Screen 03/12: connected site, four orange-number metrics and two recent
 * publication rows. Business locations and edit/refresh/search are accessible
 * from the top menu without adding a fake fifth statistics card.
 */
@Composable
fun HomePage(
    data: CatalogSnapshot,
    busy: Boolean,
    error: String?,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onOpen: (String) -> Unit,
    selectedOutlet: String = "",
) {
    var legalExpanded by rememberSaveable { mutableStateOf(false) }
    val metrics = remember(data, selectedOutlet) { homeMetrics(data, selectedOutlet) }
    val recent = remember(data, selectedOutlet) {
        homeRecentPublications(data, selectedOutlet)
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 18.dp),
    ) {
        HomeTopBar(
            busy = busy,
            onNavigate = onNavigate,
            onRefresh = onRefresh,
            onEdit = onEdit,
            onLegal = { legalExpanded = !legalExpanded },
        )
        Spacer(Modifier.height(16.dp))
        HomeConnectedCard(
            site = homeSiteLabel(data.site),
            busy = busy,
            onEdit = onEdit,
        )
        Spacer(Modifier.height(16.dp))

        metrics.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                pair.forEach { metric ->
                    HomeMetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(metric.route) },
                    )
                }
            }
            if (rowIndex == 0) Spacer(Modifier.height(9.dp))
        }

        Spacer(Modifier.height(33.dp))
        Text(
            text = "Nedavno objavljeno",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        if (recent.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = BorderStroke(1.dp, Border),
                shape = RoundedCornerShape(11.dp),
            ) {
                Text(
                    text = "Nema javno objavljenih dokumenata.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else {
            recent.forEachIndexed { index, publication ->
                HomeRecentPublication(
                    publication = publication,
                    onClick = { onOpen(publication.url) },
                )
                if (index != recent.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }

        if (busy) {
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Accent,
                )
                Spacer(Modifier.width(8.dp))
                Text("Osvježavanje javnih podataka…", color = TextMuted, fontSize = 13.sp)
            }
        }
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(11.dp))
            Text(error, color = Color(0xFFFFC997), fontSize = 13.sp)
        }
        data.warnings.take(3).forEach { warning ->
            Spacer(Modifier.height(6.dp))
            Text(warning, color = Color(0xFFFFC997), fontSize = 13.sp)
        }
        if (legalExpanded) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(containerColor = Surface),
            ) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Pravne informacije",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { legalExpanded = false }) {
                            Text("Zatvori", color = Accent)
                        }
                    }
                    val legal = data.index.legal
                    if (legal == null) {
                        Text("Pravni profil nije objavljen.", color = TextMuted, fontSize = 12.sp)
                    } else {
                        Text(
                            "Revizija: " + legal.rulesRevision,
                            color = TextMuted, fontSize = 12.sp,
                        )
                        Text(
                            "Primjena: " + legal.effectiveFrom,
                            color = TextMuted, fontSize = 12.sp,
                        )
                        Text(
                            "Arhiva: " + legal.archiveMinDays + " dana",
                            color = TextMuted, fontSize = 12.sp,
                        )
                    }
                    Text(
                        "Javno objavljeni podaci ne potvrđuju zakonsku usklađenost trgovca.",
                        color = TextMuted, fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    busy: Boolean,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onLegal: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().height(65.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_sidrena_mark),
            contentDescription = null,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = "SIDRENA",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 0.25.sp,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Opcije početne stranice",
                    tint = TextMuted,
                    modifier = Modifier.size(25.dp),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Pretraživanje") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onNavigate("search")
                    },
                )
                DropdownMenuItem(
                    text = { Text("Poslovnice") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onNavigate("locations")
                    },
                )
                DropdownMenuItem(
                    text = { Text("Osvježi podatke") },
                    leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                    enabled = !busy,
                    onClick = {
                        menuExpanded = false
                        onRefresh()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Promijeni stranicu") },
                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Pravne informacije") },
                    leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onLegal()
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeConnectedCard(site: String, busy: Boolean, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onEdit)
            .semantics { contentDescription = "Povezana stranica " + site },
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Povezana stranica",
                fontSize = 12.sp,
                color = TextMuted,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = site,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier.size(7.dp).background(Success, CircleShape),
                )
                Text(
                    text = if (busy) "Osvježavanje" else "Povezano",
                    color = Success,
                    fontSize = if (busy) 10.sp else 12.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HomeMetricCard(
    metric: HomeMetric,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = metric.label + ": " + metric.count },
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = metric.count,
                color = Accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (metric.count.length > 7) 19.sp else 27.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metric.label,
                color = TextMuted,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeRecentPublication(
    publication: HomePublication,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Otvori " + publication.title +
                    ", " + publication.format
            },
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 75.dp)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier.size(39.dp)
                    .background(SurfaceRaised, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Accent,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = publication.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = publication.format + (
                        publication.date?.takeIf(String::isNotBlank)
                            ?.let { " · " + it } ?: ""
                    ),
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun homeArchiveLabel(entry: ArchiveEntry): String = when {
    entry.kind.contains("uslug", ignoreCase = true) -> "Cjenik usluga"
    entry.kind.contains("proizvod", ignoreCase = true) -> "Cjenik proizvoda"
    else -> "Arhivirani cjenik"
}

/** Keep the legacy three-entry helper for consumers/tests; Home renders two. */
internal fun homeRecentArchive(entries: List<ArchiveEntry>): List<ArchiveEntry> =
    entries.distinctBy { it.url }
        .sortedByDescending { homeDateKey(it.date) }
        .take(3)

/** ISO and Croatian dd. MM. yyyy. strings, without guesswork for malformed dates. */
internal fun homeDateKey(raw: String): Long {
    val iso = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(raw)
    val hr = Regex("""(\d{1,2})\.\s*(\d{1,2})\.\s*(\d{4})""").find(raw)
    return runCatching {
        when {
            iso != null -> LocalDate.of(
                iso.groupValues[1].toInt(),
                iso.groupValues[2].toInt(),
                iso.groupValues[3].toInt(),
            ).toEpochDay()
            hr != null -> LocalDate.of(
                hr.groupValues[3].toInt(),
                hr.groupValues[2].toInt(),
                hr.groupValues[1].toInt(),
            ).toEpochDay()
            else -> Long.MIN_VALUE
        }
    }.getOrDefault(Long.MIN_VALUE)
}

@Preview(name = "03 SIDRENA Početna 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaHomePreview() {
    val example = CatalogSnapshot(
        site = "https://primjer-trgovina.hr",
        index = CatalogIndex(
            publishedAt = "2026-10-31",
            publicPage = "https://primjer-trgovina.hr/cjenici",
            archivePage = null,
            productsCsv = "https://primjer-trgovina.hr/proizvodi.csv",
            servicesCsv = "https://primjer-trgovina.hr/usluge.csv",
            outlets = emptyList(),
        ),
        products = emptyList(),
        services = emptyList(),
        archive = listOf(
            ArchiveEntry(
                "2026-10-30", "usluge", "", "xml",
                "https://primjer-trgovina.hr/arhiva-usluge.xml",
            ),
        ),
        warnings = emptyList(),
    )
    SidrenaTheme {
        HomePage(
            data = example,
            busy = false,
            error = null,
            onNavigate = {},
            onRefresh = {},
            onEdit = {},
            onOpen = {},
        )
    }
}

@Preview(name = "03 SIDRENA Početna compact 360x640", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
internal fun SidrenaHomeCompactPreview() {
    SidrenaHomePreview()
}
