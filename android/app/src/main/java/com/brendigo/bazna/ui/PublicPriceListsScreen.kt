package com.brendigo.bazna.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
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
import com.brendigo.bazna.data.ArchiveEntry
import com.brendigo.bazna.data.CatalogIndex
import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.SiteUrl

/**
 * 07/12 — Public price lists, matching the BAZNA reference layout.
 *
 * Every link is based on the current connected WordPress installation.
 * CSV and XML are displayed only when the API actually returns them.
 * No fictitious PDF documents, sizes, or dates are fabricated.
 */
@Composable
fun PublicPriceListsScreen(
    data: CatalogSnapshot,
    selectedOutlet: String,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onArchive: () -> Unit,
    onRefresh: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val documents = remember(data, selectedOutlet) {
        publicPriceListDocuments(data, selectedOutlet)
    }
    val visible = remember(documents, search, filter) {
        filterPublicPriceLists(documents, search, filter)
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        PublicPriceListsToolbar(
            busy = busy,
            filter = filter,
            onFilter = { filter = it },
            onSearch = { showSearch = !showSearch },
            onBack = onBack,
            onArchive = onArchive,
            onRefresh = onRefresh,
        )
        if (showSearch) {
            CatalogSearchBar(
                value = search,
                onQueryChange = { search = it.take(256) },
                hint = "Pretraži cjenike…",
                accessibilityLabel = "Pretraži javno objavljene cjenike",
                heightDp = 40,
                radiusDp = 9,
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Objavljeni dokumenti",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        if (busy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Accent,
                trackColor = SurfaceRaised,
            )
            Spacer(Modifier.height(9.dp))
        }
        if (!error.isNullOrBlank()) {
            Text(
                error,
                modifier = Modifier.padding(bottom = 12.dp),
                color = Color(0xFFFFC997),
                fontSize = 12.sp,
            )
        }
        if (data.index.pendingPublication == true) {
            Text(
                "Povezana BAZNA stranica ima neobjavljene izmjene. Cjenici mogu biti zastarjeli.",
                color = Color(0xFFFFC997),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (visible.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    if (documents.isEmpty()) "Nema javno objavljenih cjenika."
                    else "Nema cjenika za odabrani filtar.",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (documents.isEmpty())
                        "Provjerite objavu CSV/XML cjenika u BAZNA administraciji."
                    else "U izborniku odaberite Sve ili promijenite pretragu.",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(visible, key = { it.url }) { document ->
                    PublicPriceListRow(
                        document = document,
                        onOpen = { onOpen(document.url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicPriceListsToolbar(
    busy: Boolean,
    filter: Int,
    onFilter: (Int) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onArchive: () -> Unit,
    onRefresh: () -> Unit,
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
                tint = Accent,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "Javni cjenici",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Opcije javnih cjenika",
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp),
                )
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
            ) {
                listOf("Svi dokumenti", "Proizvodi", "Usluge", "Ostalo")
                    .forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                if (index == filter) Icon(
                                    Icons.Outlined.Check, contentDescription = null
                                )
                            },
                            onClick = {
                                onFilter(index)
                                menu = false
                            },
                        )
                    }
                DropdownMenuItem(
                    text = { Text("Pretraži cjenike") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    onClick = {
                        menu = false
                        onSearch()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Osvježi cjenike") },
                    enabled = !busy,
                    leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                    onClick = {
                        menu = false
                        onRefresh()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Arhiva cjenika") },
                    leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                    onClick = {
                        menu = false
                        onArchive()
                    },
                )
            }
        }
    }
}

/** Native card from reference 07, with genuine CSV/XML/WEB identity only. */
@Composable
private fun PublicPriceListRow(
    document: PublicPriceListDocument,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onOpen)
            .semantics {
                contentDescription = "Otvori " + document.title + ", " + document.format
            },
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 9.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp)
                    .background(SurfaceRaised, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    document.format,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    document.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 19.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    document.format + (document.date?.let { " • " + it } ?: ""),
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.ArrowDownward,
                    contentDescription = "Otvori dokument u pregledniku",
                    tint = Accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

internal data class PublicPriceListDocument(
    val title: String,
    val url: String,
    val format: String,
    val group: Int,
    val date: String?,
    val archived: Boolean,
)

/**
 * Build a genuine public list from the current published products/services
 * CSV endpoints, the public website, and previously published CSV/XML files.
 * Every URL must pass the same-origin HTTPS guard before reaching the UI.
 */
internal fun publicPriceListDocuments(
    data: CatalogSnapshot,
    selectedOutlet: String = "",
): List<PublicPriceListDocument> {
    val site = runCatching { SiteUrl.parse(data.site) }.getOrNull()
        ?: return emptyList()
    val results = mutableListOf<PublicPriceListDocument>()
    val outlet = data.index.outlets.firstOrNull { it.code == selectedOutlet }
    // Unknown outlet code must not silently display webshop prices as if selected.
    if (selectedOutlet.isNotBlank() && outlet == null) return emptyList()
    fun add(
        title: String,
        rawUrl: String?,
        format: String,
        group: Int,
        date: String?,
        archived: Boolean,
    ) {
        val safeUrl = site.publicLink(rawUrl) ?: return
        results.add(
            PublicPriceListDocument(
                title = title,
                url = safeUrl,
                format = format,
                group = group,
                date = date?.takeIf { it.isNotBlank() },
                archived = archived,
            ),
        )
    }

    // WordPress v1 publishes URLs even before the associated CSV file exists.
    // A confirmed available_csv=false must never create an actionable card.
    // Null means an older compatible plugin with no publication-status field.
    val productReady = if (outlet == null) data.index.productsCsvReady else outlet.productsCsvReady
    val serviceReady = if (outlet == null) data.index.servicesCsvReady else outlet.servicesCsvReady
    if (productReady != false) {
        add(
            title = "Cjenik proizvoda",
            rawUrl = outlet?.productCsv ?: data.index.productsCsv,
            format = "CSV",
            group = 1,
            date = data.index.publishedAt,
            archived = false,
        )
    }
    if (serviceReady != false) {
        add(
            title = "Cjenik usluga",
            rawUrl = outlet?.serviceCsv ?: data.index.servicesCsv,
            format = "CSV",
            group = 2,
            date = data.index.publishedAt,
            archived = false,
        )
    }
    add(
        title = "Javni cjenici — web stranica",
        rawUrl = data.index.publicPage,
        format = "WEB",
        group = 3,
        date = data.index.publishedAt,
        archived = false,
    )

    val older = data.archive.sortedByDescending { homeDateKey(it.date) }
    older.forEach { entry ->
        val format = entry.format.uppercase()
        if (format != "CSV" && format != "XML") return@forEach
        val group = priceListGroup(entry.kind)
        val name = when (group) {
            1 -> "Arhivirani cjenik proizvoda"
            2 -> "Arhivirani cjenik usluga"
            else -> "Arhivirani cjenik"
        }
        add(
            title = name,
            rawUrl = entry.url,
            format = format,
            group = group,
            date = entry.date,
            archived = true,
        )
    }

    return results.distinctBy { it.url }
}

internal fun priceListGroup(kind: String): Int = when {
    kind.contains("proizvod", ignoreCase = true) -> 1
    kind.contains("uslug", ignoreCase = true) -> 2
    else -> 3
}

internal fun filterPublicPriceLists(
    documents: List<PublicPriceListDocument>,
    search: String,
    filter: Int,
): List<PublicPriceListDocument> {
    require(filter in 0..3)
    val terms = search.trim().take(256)
        .split(Regex("\\s+")).filter { it.isNotEmpty() }
    return documents.filter { document ->
        (filter == 0 || document.group == filter) &&
            terms.all { term ->
                listOf(
                    document.title,
                    document.format,
                    document.date.orEmpty(),
                    if (document.archived) "arhiva" else "aktualni",
                ).any { it.contains(term, ignoreCase = true) }
            }
    }
}

@Preview(name = "07 Cjenici — 393×852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaPriceListsPreview() {
    val sample = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "10. 10. 2026.",
            publicPage = "https://primjer.hr/cjenici",
            archivePage = null,
            productsCsv = "https://primjer.hr/products.csv",
            servicesCsv = "https://primjer.hr/services.csv",
            outlets = emptyList(),
        ),
        products = emptyList(),
        services = emptyList(),
        archive = listOf(
            ArchiveEntry("2026-10-09", "proizvodi", "", "xml", "https://primjer.hr/09.xml"),
            ArchiveEntry("2026-10-07", "usluge", "", "csv", "https://primjer.hr/07.csv"),
        ),
        warnings = emptyList(),
    )
    SidrenaTheme {
        PublicPriceListsScreen(
            data = sample,
            selectedOutlet = "",
            busy = false,
            error = null,
            onBack = {},
            onOpen = {},
            onArchive = {},
            onRefresh = {},
        )
    }
}
