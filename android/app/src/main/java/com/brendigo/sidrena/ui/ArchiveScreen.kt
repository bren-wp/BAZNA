package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.sidrena.data.ArchiveEntry
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.SiteUrl
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * SIDRENA screen 08/12: read-only archive of actual published CSV/XML files.
 *
 * Layout follows the approved SIDRENA screen: back arrow, "Arhiva" heading,
 * compact search, dynamic year chips and document rows with format-colored
 * icons and open actions. There are no fictional PDF files or file sizes.
 */
@Composable
fun ArchivePage(
    data: CatalogSnapshot,
    busy: Boolean,
    error: String?,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onPriceLists: () -> Unit,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedYear by rememberSaveable { mutableIntStateOf(0) }
    var selectedFormat by rememberSaveable { mutableStateOf("sve") }

    val archive = remember(data) { publishedArchiveEntries(data) }
    val years = remember(archive) { archiveAvailableYears(archive) }
    val year = selectedYear.takeIf { it in years } ?: 0
    val filtered = remember(archive, query, year, selectedFormat) {
        filterPublishedArchive(archive, query, year, selectedFormat)
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        ArchiveToolbar(
            busy = busy,
            format = selectedFormat,
            onBack = onBack,
            onPriceLists = onPriceLists,
            onRefresh = onRefresh,
            onFormat = { selectedFormat = it },
        )
        Spacer(Modifier.height(7.dp))
        CatalogSearchBar(
            value = query,
            onQueryChange = { query = it.take(256) },
            hint = "Pretraži arhivu…",
            accessibilityLabel = "Pretraži arhivske cjenike",
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CatalogFilterChip(
                label = "Sve",
                selected = year == 0,
                onClick = { selectedYear = 0 },
            )
            years.forEach { available ->
                CatalogFilterChip(
                    label = available.toString(),
                    selected = year == available,
                    onClick = { selectedYear = available },
                )
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Prikazano: " + filtered.size + " / " + archive.size,
                color = TextMuted,
                fontSize = 12.sp,
            )
            Text(
                text = when (selectedFormat) {
                    "csv" -> "Format: CSV"
                    "xml" -> "Format: XML"
                    else -> "CSV i XML"
                },
                color = TextMuted,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        if (busy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Accent,
                trackColor = SurfaceRaised,
            )
            Spacer(Modifier.height(8.dp))
        }
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                modifier = Modifier.padding(bottom = 10.dp),
                color = Color(0xFFFFC997),
                fontSize = 13.sp,
            )
        }
        data.warnings
            .filter { it.contains("arhiv", ignoreCase = true) }
            .take(1)
            .forEach { warning ->
                Text(
                    text = warning,
                    color = Color(0xFFFFC997),
                    modifier = Modifier.padding(bottom = 10.dp),
                    fontSize = 13.sp,
                )
            }

        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(
                    Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = if (archive.isEmpty()) {
                        "Nema dostupnih arhivskih cjenika."
                    } else {
                        "Nema cjenika za odabrane filtre."
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (archive.isNotEmpty()) {
                    Text(
                        "Promijenite godinu, format ili pretragu.",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(items = filtered, key = { it.url }) { entry ->
                    ArchiveDocumentRow(entry = entry, onOpen = { onOpen(entry.url) })
                }
            }
        }
    }
}

@Composable
private fun ArchiveToolbar(
    busy: Boolean,
    format: String,
    onBack: () -> Unit,
    onPriceLists: () -> Unit,
    onRefresh: () -> Unit,
    onFormat: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().height(61.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Natrag na početnu",
                tint = Color.White,
                modifier = Modifier.size(25.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Arhiva",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Opcije arhive i filtriranje formata",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf(
                    "sve" to "Svi formati",
                    "csv" to "Samo CSV",
                    "xml" to "Samo XML",
                ).forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(if (format == value) "✓ " + label else label) },
                        onClick = {
                            menu = false
                            onFormat(value)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Aktualni cjenici") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                    },
                    onClick = {
                        menu = false
                        onPriceLists()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Osvježi arhivu") },
                    leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                    enabled = !busy,
                    onClick = {
                        menu = false
                        onRefresh()
                    },
                )
            }
        }
    }
}

@Composable
private fun ArchiveDocumentRow(entry: ArchiveEntry, onOpen: () -> Unit) {
    val color = if (entry.format.equals("csv", true)) {
        Color(0xFF52D9A3)
    } else {
        Color(0xFFFF852C)
    }
    val title = archiveDocumentTitle(entry)
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onOpen)
            .semantics {
                contentDescription = "Otvori " + title + ", " + entry.format.uppercase()
            },
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 79.dp)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(43.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.format.uppercase() +
                        (entry.outlet.takeIf { it.isNotBlank() }?.let { " · " + it } ?: ""),
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.date.isNotBlank()) {
                    Text(
                        text = entry.date,
                        color = Color(0xFFD8E3EF),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = "Otvori arhivski dokument",
                    tint = Color(0xFFDBE9F7),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * Validates every URL, allows only the published CSV/XML formats and returns
 * stable latest-first unique documents. Unknown dates sort after dated files.
 */
internal fun publishedArchiveEntries(data: CatalogSnapshot): List<ArchiveEntry> {
    val site = runCatching { SiteUrl.parse(data.site) }.getOrNull()
        ?: return emptyList()
    return data.archive.asSequence()
        .filter { it.format.equals("csv", true) || it.format.equals("xml", true) }
        .mapNotNull { entry ->
            val safe = site.publicLink(entry.url) ?: return@mapNotNull null
            entry.copy(
                format = entry.format.lowercase(Locale.ROOT),
                url = safe,
            )
        }
        .distinctBy { it.url }
        .sortedByDescending { homeDateKey(it.date) }
        .toList()
}

/** Null means an unknown/invalid date, not a fictional year. */
internal fun archiveEntryYear(entry: ArchiveEntry): Int? {
    val key = homeDateKey(entry.date)
    if (key == Long.MIN_VALUE) return null
    return runCatching { LocalDate.ofEpochDay(key).year }.getOrNull()
}

/** Only years present in actual public archive records become selectable. */
internal fun archiveAvailableYears(items: List<ArchiveEntry>): List<Int> =
    items.mapNotNull(::archiveEntryYear).distinct().sortedDescending()

internal fun filterPublishedArchive(
    items: List<ArchiveEntry>,
    query: String,
    selectedYear: Int = 0,
    format: String = "sve",
): List<ArchiveEntry> {
    require(format in setOf("sve", "csv", "xml"))
    val terms = query.trim().take(256)
        .split(Regex("\\s+")).filter(String::isNotEmpty)
    return items.filter { entry ->
        (selectedYear == 0 || archiveEntryYear(entry) == selectedYear) &&
            (format == "sve" || entry.format.equals(format, true)) &&
            terms.all { term ->
                listOf(
                    entry.date,
                    entry.kind,
                    entry.outlet,
                    entry.format,
                    archiveDocumentTitle(entry),
                ).any { text -> text.contains(term, ignoreCase = true) }
            }
    }
}

/** Names are based only on the published kind and parsable publication date. */
internal fun archiveDocumentTitle(entry: ArchiveEntry): String {
    val kind = when (priceListGroup(entry.kind)) {
        1 -> "Cjenik proizvoda"
        2 -> "Cjenik usluga"
        else -> "Cjenik"
    }
    val key = homeDateKey(entry.date)
    if (key == Long.MIN_VALUE) return kind
    return runCatching {
        val date = LocalDate.ofEpochDay(key)
        val month = date.month.getDisplayName(
            TextStyle.FULL_STANDALONE, Locale.forLanguageTag("hr-HR"),
        )
        kind + " — " + month + " " + date.year + "."
    }.getOrDefault(kind)
}

@Preview(name = "08 SIDRENA Arhiva 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaArchivePreview() {
    val example = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "2026-10-11",
            publicPage = "https://primjer.hr/cjenici",
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = emptyList(),
        ),
        products = emptyList(),
        services = emptyList(),
        archive = listOf(
            ArchiveEntry("2026-10-10", "proizvodi", "", "csv", "https://primjer.hr/a.csv"),
            ArchiveEntry("2026-09-09", "usluge", "", "xml", "https://primjer.hr/b.xml"),
            ArchiveEntry("2025-12-08", "proizvodi", "", "csv", "https://primjer.hr/c.csv"),
            ArchiveEntry("2024-11-07", "usluge", "", "xml", "https://primjer.hr/d.xml"),
        ),
        warnings = emptyList(),
    )
    SidrenaTheme {
        ArchivePage(
            data = example,
            busy = false,
            error = null,
            onOpen = {},
            onBack = {},
            onPriceLists = {},
            onRefresh = {},
        )
    }
}
