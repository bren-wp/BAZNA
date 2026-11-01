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
import androidx.compose.foundation.shape.CircleShape
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
 * 04/12: SIDRENA products.
 * The public API has no images, sale flags, "new" flags, or category data.
 * Keep the reference layout but use truthful anchor-price filters and local
 * product icons rather than fabricated metadata or external image requests.
 */
@Composable
fun ProductCatalogScreen(
    data: CatalogSnapshot,
    busy: Boolean,
    onDetails: (PriceItem) -> Unit,
    onBack: () -> Unit,
    onShowServices: () -> Unit,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val visible = remember(data.products, query, filter) {
        CatalogSearch.items(data.products, query, filter)
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
        ProductSearchBar(query, onQueryChange = { query = it.take(256) })
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Svi", "Sidrena", "Bez sidrene").forEachIndexed { index, label ->
                ProductFilterChip(
                    label = label,
                    selected = filter == index,
                    onClick = { filter = index },
                )
            }
            ProductFilterChip("Usluge ▾", selected = false, onClick = onShowServices)
        }

        Spacer(Modifier.height(14.dp))
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
                    ProductReferenceRow(product, onClick = { onDetails(product) })
                }
            }
        }
    }
}

@Composable
private fun ProductSearchBar(value: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .height(52.dp)
            .background(SurfaceRaised, RoundedCornerShape(12.dp))
            .semantics { contentDescription = "Pretraži proizvode" }
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
                        Text("Pretraži proizvode…", color = TextMuted, fontSize = 15.sp)
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
private fun ProductFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun ProductReferenceRow(item: PriceItem, onClick: () -> Unit) {
    val currentPrice = formatPrice(item.currentPrice)
    val referencePrice = formatPrice(item.referencePrice)
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
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // A neutral local pictogram, not a fake remote product photo.
            Box(
                modifier = Modifier.size(width = 64.dp, height = 72.dp)
                    .background(Color(0xFFF0EEE8), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = Color(0xFF996F4D),
                    modifier = Modifier.size(35.dp),
                )
            }
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
                val unitText = item.unit.ifBlank {
                    item.sku.takeIf { it.isNotBlank() }?.let { "SKU: " + it }.orEmpty()
                }
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
