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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Inventory2
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
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSearch
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.PriceItem

/**
 * Screen 05/12 — SIDRENA public service catalog.
 *
 * Reference layout: back arrow, title and overflow menu, search bar,
 * horizontal chips, vertically stacked picture-sized service rows.
 *
 * Public CSV does not carry thumbnails, popularity, or category fields:
 * a local tool icon substitutes images; filtering is restricted to published
 * anchor-price data, never an inferred "popular" or "category" value.
 */
@Composable
fun ServiceCatalogScreen(
    data: CatalogSnapshot,
    busy: Boolean,
    error: String?,
    onDetails: (PriceItem) -> Unit,
    onBack: () -> Unit,
    onShowProducts: () -> Unit,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val visible = remember(data.services, query, filter) {
        visiblePublicServices(data.services, query, filter)
    }

    Column(modifier = Modifier.fillMaxSize().background(Background).imePadding()) {
        ServiceToolbar(
            busy = busy,
            onBack = onBack,
            onShowProducts = onShowProducts,
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(7.dp))
        CatalogSearchBar(
            value = query,
            onQueryChange = { query = it.take(256) },
            hint = "Pretraži usluge…",
            accessibilityLabel = "Pretraži javno objavljene usluge",
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Svi", "Sidrena", "Bez sidrene").forEachIndexed { index, title ->
                CatalogFilterChip(
                    label = title,
                    selected = filter == index,
                    onClick = { filter = index },
                )
            }
            CatalogFilterChip(
                label = "Proizvodi ▾",
                selected = false,
                onClick = onShowProducts,
            )
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
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = if (data.services.isEmpty()) "Nema javno objavljenih usluga."
                    else "Nema usluga za odabrane filtre.",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                if (query.isNotBlank() || filter != 0) {
                    Text(
                        "Promijenite pretragu ili odaberite Svi.",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(
                    items = visible,
                    key = { it.id + "|" + it.name },
                ) { service ->
                    ServiceReferenceCard(item = service, onClick = { onDetails(service) })
                }
            }
        }
    }
}

@Composable
private fun ServiceToolbar(
    busy: Boolean,
    onBack: () -> Unit,
    onShowProducts: () -> Unit,
    onRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().height(61.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Natrag na početnu",
                tint = Color.White,
                modifier = Modifier.size(25.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Usluge",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Opcije usluga",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Osvježi usluge") },
                    leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                    enabled = !busy,
                    onClick = {
                        expanded = false
                        onRefresh()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Prikaži proizvode") },
                    leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onShowProducts()
                    },
                )
            }
        }
    }
}

@Composable
private fun ServiceReferenceCard(item: PriceItem, onClick: () -> Unit) {
    val price = serviceCurrentPrice(item.currentPrice)
    val anchor = serviceReferenceCaption(item.referencePrice)
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = item.name + ", cijena " + price +
                    (anchor?.let { ", " + it } ?: "")
            },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.65f)),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 101.dp)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 82.dp)
                    .background(SurfaceRaised, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(34.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = item.name.ifBlank { "Usluga bez naziva" },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = price,
                    color = if (item.currentPrice.isBlank()) TextMuted else Accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (anchor != null) {
                    Text(
                        text = anchor,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = "Sidrena cijena nije objavljena",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Sorting stays in server order; only public fields decide filtering. */
internal fun visiblePublicServices(
    items: List<PriceItem>,
    query: String,
    anchorFilter: Int,
): List<PriceItem> = CatalogSearch.items(items, query, anchorFilter)

/** A CSV price is an actual published price, never automatically a "from" price. */
internal fun serviceCurrentPrice(raw: String): String =
    if (raw.isBlank()) "Cijena nije objavljena" else formatPrice(raw)

internal fun serviceReferenceCaption(raw: String): String? =
    if (raw.isBlank()) null else "Ref. cijena: " + formatPrice(raw)

@Preview(name = "05 SIDRENA Usluge 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaServicePreview() {
    val preview = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "2026-10-10",
            publicPage = null,
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = emptyList(),
        ),
        products = emptyList(),
        services = listOf(
            PriceItem("1", "Servis klima-uređaja", "usluga", "45,00", "60,00", "2026-10-01", "", "", ""),
            PriceItem("2", "Montaža rasvjete", "usluga", "30,00", "45,00", "2026-10-01", "", "", ""),
            PriceItem("3", "Čišćenje prostora", "usluga", "20,00", "35,00", "2026-10-01", "", "", ""),
            PriceItem("4", "Izrada web stranice", "usluga", "250,00", "350,00", "2026-10-01", "", "", ""),
            PriceItem("5", "Knjigovodstvene usluge", "usluga", "100,00", "150,00", "2026-10-01", "", "", ""),
            PriceItem("6", "Dizajn logotipa", "usluga", "80,00", "120,00", "2026-10-01", "", "", ""),
        ),
        archive = emptyList(),
        warnings = emptyList(),
    )
    SidrenaTheme {
        ServiceCatalogScreen(
            data = preview,
            busy = false,
            error = null,
            onDetails = {},
            onBack = {},
            onShowProducts = {},
            onRefresh = {},
        )
    }
}
