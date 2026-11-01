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

/**
 * 07/12 — Public price lists, matching the SIDRENA reference layout.
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
            onBack = onBack,
            onArchive = onArchive,
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(7.dp))
        CatalogSearchBar(
            value = search,
            onQueryChange = { search = it.take(256) },
            hint = "Pretraži cjenike…",
            accessibilityLabel = "Pretraži javne cjenike",
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Svi", "Proizvodi", "Usluge", "Ostalo").forEachIndexed { index, title ->
                CatalogFilterChip(
                    label = title,
                    selected = filter == index,
                    onClick = { filter = index },
                )
            }
        }
        Spacer(Modifier.height(14.dp))

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
                color = Color(0xFFFFC997),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (visible.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = if (documents.isEmpty()) {
                        "Nema javno objavljenih cjenika."
                    } else {
                        "Nema cjenika za odabranu pretragu."
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (documents.isNotEmpty()) {
                    Text(
                        "Promijenite pojam ili odaberite Svi.",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
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
                tint = Color.White,
                modifier = Modifier.size(25.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Cjenici",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Opcije cjenika",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
            ) {
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

@Composable
private fun PublicPriceListRow(
    document: PublicPriceListDocument,
    onOpen: () -> Unit,
) {
    val tone = when (document.format) {
        "CSV" -> Color(0xFF52D9A3)
        "XML" -> Color(0xFFFF852C)
        else -> SoftBlue
    }
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onOpen)
            .semantics {
                contentDescription = "Otvori " + document.title + ", " + document.format
            },
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp)
                    .background(tone.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = document.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = document.format + " · " +
                        (if (document.archived) "Arhiva" else "Aktualni cjenik"),
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                document.date?.let { date ->
                    Text(
                        text = date,
                        color = Color(0xFFD8E3EF),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = "Otvori dokument",
                    tint = Color(0xFFDBE9F7),
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

    add(
        title = "Cjenik proizvoda",
        rawUrl = outlet?.productCsv ?: data.index.productsCsv,
        format = "CSV",
        group = 1,
        date = data.index.publishedAt,
        archived = false,
    )
    add(
        title = "Cjenik usluga",
        rawUrl = outlet?.serviceCsv ?: data.index.servicesCsv,
        format = "CSV",
        group = 2,
        date = data.index.publishedAt,
        archived = false,
    )
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
