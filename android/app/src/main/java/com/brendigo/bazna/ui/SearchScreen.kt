package com.brendigo.bazna.ui

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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
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
import com.brendigo.bazna.data.CatalogIndex
import com.brendigo.bazna.data.CatalogSearch
import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.PriceItem

/** Search category is determined by the actual product/service feed, not a guessed label. */
internal data class SearchCatalogEntry(
    val item: PriceItem,
    val isService: Boolean,
)

/** Pure JVM-testable filter. Keeps source ordering and supports multi-term search. */
internal fun searchCatalogEntries(
    products: List<PriceItem>,
    services: List<PriceItem>,
    query: String,
    kind: Int,
): List<SearchCatalogEntry> {
    require(kind in 0..2)
    val term = query.trim().take(256)
    val productEntries = if (kind != 2) {
        CatalogSearch.items(products, term).map { SearchCatalogEntry(it, false) }
    } else {
        emptyList()
    }
    val serviceEntries = if (kind != 1) {
        CatalogSearch.items(services, term).map { SearchCatalogEntry(it, true) }
    } else {
        emptyList()
    }
    return productEntries + serviceEntries
}

internal fun searchResultPrice(item: PriceItem): String =
    if (item.currentPrice.isBlank()) "Cijena nije objavljena"
    else formatPrice(item.currentPrice)

internal fun searchReferencePrice(item: PriceItem): String? =
    item.referencePrice.takeIf { it.isNotBlank() }?.let { "Bazna: " + formatPrice(it) }

/**
 * SIDRENA Android screen 10/12 — unified public catalog search.
 *
 * UI follows the reference: dark navy, back arrow + title, search box,
 * compact orange active filter, real results and read-only detail navigation.
 * No product thumbnails, categories, discounts, popularity, or fake data.
 */
@Composable
fun SearchPage(
    data: CatalogSnapshot,
    busy: Boolean,
    error: String?,
    query: String,
    kind: Int,
    onQueryChange: (String) -> Unit,
    onKindChange: (Int) -> Unit,
    onDetails: (PriceItem) -> Unit,
    onBack: () -> Unit,
    onArchive: () -> Unit,
    onRefresh: () -> Unit,
) {
    val results = remember(data.products, data.services, query, kind) {
        searchCatalogEntries(data.products, data.services, query, kind)
    }
    val total = data.products.size + data.services.size

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Background)
            .imePadding(),
    ) {
        SearchToolbar(
            busy = busy,
            onBack = onBack,
            onArchive = onArchive,
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(7.dp))
        CatalogSearchBar(
            value = query,
            onQueryChange = { onQueryChange(it.take(256)) },
            hint = "Pretraži proizvode i usluge…",
            accessibilityLabel = "Pretraži proizvode i usluge po nazivu, šifri ili oznaci",
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Sve", "Proizvodi", "Usluge").forEachIndexed { index, title ->
                CatalogFilterChip(
                    label = title,
                    selected = kind == index,
                    onClick = { onKindChange(index) },
                )
            }
            CatalogFilterChip(
                label = "Arhiva ↗",
                selected = false,
                onClick = onArchive,
            )
        }
        Spacer(Modifier.height(11.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Pronađeno " + results.size + " rezultata",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Ukupno " + total,
                color = TextMuted,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(11.dp))
        if (busy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Accent,
                trackColor = SurfaceRaised,
            )
            Spacer(Modifier.height(10.dp))
        }
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                modifier = Modifier.padding(bottom = 11.dp),
                color = Color(0xFFFFC997),
                fontSize = 13.sp,
            )
        }
        if (results.isEmpty()) {
            SearchEmptyState(
                catalogEmpty = total == 0,
                hasQuery = query.isNotBlank(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(results) { result ->
                    SearchResultCard(result, onClick = { onDetails(result.item) })
                }
            }
        }
    }
}

@Composable
private fun SearchToolbar(
    busy: Boolean,
    onBack: () -> Unit,
    onArchive: () -> Unit,
    onRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().height(61.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Natrag na prethodni zaslon",
                tint = Color.White,
                modifier = Modifier.size(25.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Pretraživanje",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Opcije pretrage",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Osvježi podatke") },
                    leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                    enabled = !busy,
                    onClick = {
                        expanded = false
                        onRefresh()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Arhivski cjenici") },
                    leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onArchive()
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    entry: SearchCatalogEntry,
    onClick: () -> Unit,
) {
    val price = searchResultPrice(entry.item)
    val reference = searchReferencePrice(entry.item)
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = (if (entry.isService) "Usluga " else "Proizvod ") +
                    entry.item.name + ", " + price +
                    (reference?.let { ", " + it } ?: "")
            },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 91.dp)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(59.dp)
                    .background(SurfaceRaised, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (entry.isService) {
                        Icons.Outlined.Build
                    } else {
                        Icons.Outlined.Inventory2
                    },
                    contentDescription = null,
                    tint = if (entry.isService) SoftBlue else Accent,
                    modifier = Modifier.size(31.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = entry.item.name.ifBlank {
                        if (entry.isService) "Usluga bez naziva" else "Proizvod bez naziva"
                    },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (entry.isService) "Usluga" else "Proizvod",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
                if (reference != null) {
                    Text(
                        text = reference,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = price,
                    color = if (entry.item.currentPrice.isBlank()) TextMuted else Accent,
                    fontSize = if (entry.item.currentPrice.isBlank()) 11.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchEmptyState(catalogEmpty: Boolean, hasQuery: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(38.dp),
        )
        Text(
            text = if (catalogEmpty) "Nema javno objavljenih stavki."
            else "Nema rezultata pretraživanja.",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Text(
            text = if (catalogEmpty) "Provjerite povezanu stranicu i pokušajte osvježiti."
            else if (hasQuery) "Promijenite pojam ili odaberite drugi filtar."
            else "Odaberite Sve, Proizvodi ili Usluge.",
            color = TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Preview(name = "10 BAZNA Search 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaSearchPreview() {
    val example = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = null,
            publicPage = null,
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = emptyList(),
        ),
        products = listOf(
            PriceItem("p1", "Maslinovo ulje", "proizvod", "5,99", "8,49", "2026-10-10", "MU-01", "", "1 L"),
            PriceItem("p2", "Tjestenina", "proizvod", "1,49", "", "", "TJ-01", "", "500 g"),
            PriceItem("p3", "Sok od naranče", "proizvod", "2,49", "3,49", "2026-10-10", "SO-01", "", "1 L"),
        ),
        services = listOf(
            PriceItem("s1", "Montaža rasvjete", "usluga", "30,00", "45,00", "2026-10-10", "MR-01", "", ""),
            PriceItem("s2", "Servis klima-uređaja", "usluga", "45,00", "", "", "SK-01", "", ""),
        ),
        archive = emptyList(),
        warnings = emptyList(),
    )
    SidrenaTheme {
        SearchPage(
            data = example,
            busy = false,
            error = null,
            query = "",
            kind = 0,
            onQueryChange = {},
            onKindChange = {},
            onDetails = {},
            onBack = {},
            onArchive = {},
            onRefresh = {},
        )
    }
}

@Preview(name = "10 BAZNA Search compact", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
internal fun SidrenaSearchCompactPreview() {
    SidrenaSearchPreview()
}
