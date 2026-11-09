package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
 * Screen 05/12: Usluge — the reference's title, wide search box and compact
 * vertically stacked navy service cards. The screenshot is a *layout* guide;
 * names, prices, and publication status always come from the WordPress CSV.
 *
 * The reference's "od" label is not applied to a single exact published price:
 * that would falsely imply a variable starting price. The real anchor and
 * historical reference date are shown only when present in the public feed.
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
    selectedOutlet: String = "",
) {
    var query by rememberSaveable { mutableStateOf("") }
    var anchorFilter by rememberSaveable { mutableIntStateOf(0) }
    val visible = remember(data.services, query, anchorFilter) {
        visiblePublicServices(data.services, query, anchorFilter)
    }
    val publication = servicePublicationState(data, selectedOutlet)

    Column(modifier = Modifier.fillMaxSize().background(Background).imePadding()) {
        ServiceToolbar(
            busy = busy,
            anchorFilter = anchorFilter,
            onFilter = { anchorFilter = it },
            onBack = onBack,
            onShowProducts = onShowProducts,
            onRefresh = onRefresh,
        )
        // Match the shared 52 dp search field used on screen 04.
        CatalogSearchBar(
            value = query,
            onQueryChange = { query = it.take(256) },
            hint = "Pretraži usluge…",
            accessibilityLabel = "Pretraži javno objavljene usluge",
            heightDp = 52,
            radiusDp = 12,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Sve", "S baznom", "Bez bazne").forEachIndexed { index, label ->
                CatalogFilterChip(
                    label = label,
                    selected = anchorFilter == index,
                    onClick = { anchorFilter = index },
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
            Spacer(Modifier.height(10.dp))
        }

        if (!error.isNullOrBlank()) {
            Text(
                error,
                modifier = Modifier.padding(bottom = 10.dp),
                color = Color(0xFFFFC997),
                fontSize = 13.sp,
            )
        }

        if (publication == false && data.services.isEmpty()) {
            ServiceEmptyState(
                title = "CSV cjenik usluga još nije objavljen.",
                description = "Objavite cjenik u WordPress dodatku BAZNA, a zatim osvježite podatke.",
            )
        } else if (visible.isEmpty()) {
            ServiceEmptyState(
                title = if (data.services.isEmpty()) "Nema javno objavljenih usluga."
                    else "Nema usluga za odabranu pretragu.",
                description = if (query.isNotBlank() || anchorFilter != 0)
                    "Promijenite pretragu ili u izborniku odaberite Sve usluge."
                else "Provjerite javnu objavu usluga na povezanoj WordPress stranici.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 1.dp, bottom = 18.dp),
            ) {
                items(visible) { service ->
                    ServiceReferenceCard(
                        item = service,
                        site = data.site,
                        onClick = { onDetails(service) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceToolbar(
    busy: Boolean,
    anchorFilter: Int,
    onFilter: (Int) -> Unit,
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
        Spacer(Modifier.width(7.dp))
        Text(
            "Usluge",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f),
        )

        Box {
            IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Opcije usluga i filtriranje",
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("Sve usluge", "S baznom cijenom", "Bez bazne cijene")
                    .forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                if (anchorFilter == index) {
                                    Icon(Icons.Outlined.Check, contentDescription = null)
                                }
                            },
                            onClick = {
                                onFilter(index)
                                expanded = false
                            },
                        )
                    }
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
private fun ServiceEmptyState(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, start = 7.dp, end = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            Icons.Outlined.Build,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(33.dp),
        )
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(description, color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun ServiceReferenceCard(item: PriceItem, site: String, onClick: () -> Unit) {
    val current = serviceCurrentPrice(item.currentPrice)
    val anchor = serviceReferenceWithDate(item.referencePrice, item.referenceDate)

    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = item.name + ", trenutačna cijena " + current +
                    (anchor?.let { ", " + it } ?: "")
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // The reference uses a 76 × 82 dp service emblem. Use the
            // compact variant only when its text column would be clipped.
            val compact = maxWidth < 300.dp
            Row(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = 101.dp)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                if (approvedProductImageUrl(site, item.imageUrl) != null) {
                    // Use a genuine public service photo when a WordPress feed
                    // supplies one. The shared HTTPS/same-origin image loader
                    // enforces MIME, DNS, byte and decoded-pixel limits.
                    ProductThumbnail(
                        site = site,
                        imageUrl = item.imageUrl,
                        modifier = Modifier.size(
                            width = if (compact) 52.dp else 76.dp,
                            height = if (compact) 62.dp else 82.dp,
                        ),
                    )
                } else {
                Box(
                    modifier = Modifier
                        .size(
                            width = if (compact) 52.dp else 76.dp,
                            height = if (compact) 62.dp else 82.dp,
                        )
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF214364), Color(0xFF142B46)),
                            ),
                            RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Build,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(if (compact) 26.dp else 34.dp),
                    )
                }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        item.name.ifBlank { "Usluga bez naziva" },
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Cijena",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                    Text(
                        current,
                        color = if (item.currentPrice.isBlank()) TextMuted else Accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (anchor != null) {
                        Text(
                            anchor,
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
}

/** Preserve the WordPress publisher's order; use only public v1 fields. */
internal fun visiblePublicServices(
    items: List<PriceItem>,
    query: String,
    anchorFilter: Int,
): List<PriceItem> = CatalogSearch.items(items, query, anchorFilter)

/** A published exact service price is not a variable "starting from" price. */
internal fun serviceCurrentPrice(raw: String): String =
    if (raw.isBlank()) "Cijena nije objavljena" else formatPrice(raw)

internal fun serviceReferenceCaption(raw: String): String? =
    if (raw.isBlank()) null else "Ref. cijena: " + formatPrice(raw)

/** Date is kept alongside an anchored price whenever one was published. */
internal fun serviceReferenceWithDate(price: String, date: String): String? =
    serviceReferenceCaption(price)?.let {
        if (date.isBlank()) it else "$it · $date"
    }

/**
 * null = older WordPress plugin does not expose the optional readiness field.
 * Outlet CSV readiness must never borrow the webshop's flag.
 */
internal fun servicePublicationState(data: CatalogSnapshot, outletCode: String = ""): Boolean? {
    if (outletCode.isEmpty()) return data.index.servicesCsvReady
    val outlet = data.index.outlets.firstOrNull { it.code == outletCode } ?: return false
    return outlet.servicesCsvReady
}

@Preview(name = "05 BAZNA Usluge 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaServicePreview() {
    val demo = CatalogSnapshot(
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
            PriceItem("1", "Montaža namještaja", "usluga", "30,00", "", "", "", "", ""),
            PriceItem("2", "Servis klima-uređaja", "usluga", "45,00", "", "", "", "", ""),
            PriceItem("3", "Čišćenje prostora", "usluga", "20,00", "", "", "", "", ""),
            PriceItem("4", "Dostava i montaža", "usluga", "15,00", "", "", "", "", ""),
        ),
        archive = emptyList(),
        warnings = emptyList(),
    )
    SidrenaTheme {
        ServiceCatalogScreen(
            data = demo,
            busy = false,
            error = null,
            onDetails = {},
            onBack = {},
            onShowProducts = {},
            onRefresh = {},
        )
    }
}
