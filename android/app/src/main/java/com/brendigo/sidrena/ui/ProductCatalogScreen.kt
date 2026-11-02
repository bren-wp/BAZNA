package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSearch
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.PriceItem

/**
 * Filters are based on actual public CSV fields. Do not simulate
 * `Akcija`, `Novo` or categories absent from the v1 REST contract.
 */
internal fun filterProductCatalog(
    products: List<PriceItem>,
    query: String,
    filter: Int,
): List<PriceItem> {
    require(filter in 0..2)
    val queried = CatalogSearch.items(products, query, if (filter == 1) 1 else 0)
    return if (filter == 2) {
        queried.filter { item ->
            item.availability.trim().lowercase() in setOf(
                "dostupno", "na zalihi", "instock", "in stock",
            )
        }
    } else queried
}

/**
 * 04/12: SIDRENA products.
 * The WordPress v1 public CSV optionally exposes real product thumbnails.
 * There are no guaranteed sale badges, new-product flags or category fields.
 * Preserve the reference layout without inventing photos or product metadata.
 */
@Composable
fun ProductCatalogScreen(
    data: CatalogSnapshot,
    busy: Boolean,
    error: String?,
    onDetails: (PriceItem) -> Unit,
    onBack: () -> Unit,
    onShowServices: () -> Unit,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val visible = remember(data.products, query, filter) {
        filterProductCatalog(data.products, query, filter)
    }

    Column(Modifier.fillMaxSize().background(Background)) {
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
                "Proizvodi",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.weight(1f),
            )
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Opcije kataloga",
                        tint = Color.White,
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Osvježi katalog") },
                        leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                        enabled = !busy,
                        onClick = {
                            expanded = false
                            onRefresh()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Prikaži usluge") },
                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onShowServices()
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(7.dp))
        CatalogSearchBar(query, onQueryChange = { query = it.take(256) }, hint = "Pretraži proizvode…", accessibilityLabel = "Pretraži proizvode")
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Svi", "Sa sidrenom cijenom", "Dostupni").forEachIndexed { index, label ->
                CatalogFilterChip(
                    label = label,
                    selected = filter == index,
                    onClick = { filter = index },
                )
            }
            CatalogFilterChip("Usluge ▾", selected = false, onClick = onShowServices)
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
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    if (data.products.isEmpty()) "Nema objavljenih proizvoda."
                    else "Nema proizvoda za odabrane filtre.",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                if (query.isNotEmpty() || filter != 0) {
                    Text("Promijenite pretragu ili odaberite Svi.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(visible) { product ->
                    ProductReferenceRow(product, site = data.site, onClick = { onDetails(product) })
                }
            }
        }
    }
}

@Composable
internal fun CatalogSearchBar(
    value: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    accessibilityLabel: String,
    heightDp: Int = 52,
    radiusDp: Int = 12,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .height(heightDp.dp)
            .background(SurfaceRaised, RoundedCornerShape(radiusDp.dp))
            .semantics { contentDescription = accessibilityLabel }
            .padding(start = 13.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = Color(0xFFDCE8F2),
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.width(11.dp))
        BasicTextField(
            value = value,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            decorationBox = { content ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(hint, color = TextMuted, fontSize = 15.sp)
                    }
                    content()
                }
            },
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    Icons.Outlined.Clear,
                    contentDescription = "Očisti pretragu",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
internal fun CatalogFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = if (selected) Accent else SurfaceRaised,
        contentColor = if (selected) Background else Color.White,
        border = if (selected) null else BorderStroke(1.dp, Border),
    ) {
        Box(
            modifier = Modifier.height(43.dp).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProductReferenceRow(item: PriceItem, site: String, onClick: () -> Unit) {
    val currentPrice = formatPrice(item.currentPrice)
    val referencePrice = formatPrice(item.referencePrice)
    val unitText = item.unit.ifBlank {
        item.sku.takeIf { it.isNotBlank() }?.let { "SKU: " + it }.orEmpty()
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .semantics {
                contentDescription = item.name + ", trenutačna cijena " + currentPrice +
                    if (item.referencePrice.isBlank()) "" else ", sidrena cijena " + referencePrice
            },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Below 360dp device width, reserving a fixed photo and two-price
            // column would leave virtually no width for the product title.
            // Keep the reference's horizontal card unchanged for normal phones.
            if (maxWidth < 310.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ProductThumbnail(
                            site = site,
                            imageUrl = item.imageUrl,
                            modifier = Modifier.size(width = 50.dp, height = 56.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name.ifBlank { "Bez naziva" },
                                color = Color.White, fontSize = 15.sp,
                                lineHeight = 18.sp, fontWeight = FontWeight.Bold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            if (unitText.isNotBlank()) {
                                Text(
                                    unitText, color = TextMuted, fontSize = 12.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            currentPrice,
                            color = if (item.currentPrice.isBlank()) TextMuted else Accent,
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (item.referencePrice.isBlank()) "Bez sidrene"
                            else "Ref: " + referencePrice,
                            color = TextMuted, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp)
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProductThumbnail(
                        site = site,
                        imageUrl = item.imageUrl,
                        modifier = Modifier.size(width = 64.dp, height = 72.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            item.name.ifBlank { "Bez naziva" },
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (unitText.isNotBlank()) {
                            Text(
                                unitText,
                                color = Color(0xFFE1EAF3),
                                fontSize = 13.sp,
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
                            currentPrice,
                            color = if (item.currentPrice.isBlank()) TextMuted else Accent,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                        Text(
                            if (item.referencePrice.isBlank()) "Bez sidrene cijene"
                            else "Ref. cijena: " + referencePrice,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "04 Proizvodi 393×852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaProductsPreview() {
    val demo = CatalogSnapshot(
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
            PriceItem("1", "Maslinovo ulje", "proizvod", "5,99", "8,49", "2026-09-10", "A1", "", "1 L"),
            PriceItem("2", "Tjestenina", "proizvod", "1,29", "1,99", "2026-09-10", "A2", "", "500 g"),
            PriceItem("3", "Sok naranča", "proizvod", "2,49", "3,19", "2026-09-10", "A3", "", "1 L"),
            PriceItem("4", "Kava", "proizvod", "3,99", "5,49", "2026-09-10", "A4", "", "250 g"),
            PriceItem("5", "Čokolada", "proizvod", "1,19", "1,89", "2026-09-10", "A5", "", "100 g"),
            PriceItem("6", "Šampon", "proizvod", "2,89", "3,99", "2026-09-10", "A6", "", "750 ml"),
        ),
        services = emptyList(),
        archive = emptyList(),
        warnings = emptyList(),
    )
    SidrenaTheme {
        ProductCatalogScreen(
            data = demo,
            busy = false,
            error = null,
            onDetails = {},
            onBack = {},
            onShowServices = {},
            onRefresh = {},
        )
    }
}

@Preview(name = "04 Proizvodi kompaktno", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
internal fun SidrenaProductsCompactPreview() {
    SidrenaProductsPreview()
}
