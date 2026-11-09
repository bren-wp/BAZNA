package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.Outlet

/** UI representation of the public business/outlet feed; no invented geodata. */
internal data class LocationOption(
    val code: String,
    val name: String,
    val isWebshop: Boolean,
    val address: String = "",
)

internal fun publishedLocations(outlets: List<Outlet>): List<LocationOption> =
    listOf(LocationOption(code = "", name = "Webshop", isWebshop = true)) +
        outlets.map { LocationOption(code = it.code, name = it.name, isWebshop = false, address = it.address) }

/** Case-insensitive, multi-term search against actual published names/codes. */
internal fun filterPublishedLocations(
    entries: List<LocationOption>,
    query: String,
): List<LocationOption> {
    val terms = query.trim().take(256)
        .split(Regex("\\s+")).filter(String::isNotEmpty)
    return entries.filter { option ->
        terms.all { term ->
            option.name.contains(term, ignoreCase = true) ||
                option.code.contains(term, ignoreCase = true) ||
                option.address.contains(term, ignoreCase = true) ||
                (option.isWebshop && "online trgovina".contains(term, ignoreCase = true))
        }
    }
}

/**
 * Screen 09/12: Poslovnice, inspired by the ninth BAZNA reference screen.
 *
 * The public BAZNA API can return a configured business address, but has no
 * verified opening hours, coordinates, phone number or map tiles. Therefore
 * this screen never draws fabricated pins or misleading working hours.
 */
@Composable
fun LocationsPage(
    data: CatalogSnapshot,
    selectedOutlet: String,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onPriceLists: () -> Unit,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val options = remember(data.index.outlets) { publishedLocations(data.index.outlets) }
    val filtered = remember(options, query) { filterPublishedLocations(options, query) }

    Column(Modifier.fillMaxSize().background(Background).imePadding()) {
        LocationsToolbar(
            busy = busy,
            onBack = onBack,
            onPriceLists = onPriceLists,
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(7.dp))
        CatalogSearchBar(
            value = query,
            onQueryChange = { query = it.take(256) },
            hint = "Pretraži poslovnice…",
            accessibilityLabel = "Pretraži poslovnice po nazivu ili šifri",
        )
        Spacer(Modifier.height(12.dp))
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
                color = Color(0xFFFFC997),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LocationsMapPlaceholder()
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Poslovnice",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Prikazano " + filtered.size + " · Poslovnica " +
                            data.index.outlets.size,
                        color = TextMuted,
                        fontSize = 11.sp,
                    )
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        border = BorderStroke(1.dp, Border),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = "Nema poslovnica za ovu pretragu.",
                            color = TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            } else {
                items(items = filtered, key = { if (it.isWebshop) "webshop" else "outlet-" + it.code }) { entry ->
                    LocationReferenceCard(
                        option = entry,
                        selected = entry.code == selectedOutlet,
                        busy = busy,
                        onSelect = { onSelect(entry.code) },
                        onPriceLists = onPriceLists,
                    )
                }
            }
            item {
                Text(
                    text = "Adrese se prikazuju samo kada su objavljene u WordPressu. Radno vrijeme i koordinate nisu dio javnog BAZNA API-ja.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun LocationsToolbar(
    busy: Boolean,
    onBack: () -> Unit,
    onPriceLists: () -> Unit,
    onRefresh: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
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
            text = "Poslovnice",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Opcije poslovnica",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Osvježi popis") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Sync, contentDescription = null)
                    },
                    enabled = !busy,
                    onClick = {
                        menu = false
                        onRefresh()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Javni cjenici") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                    },
                    onClick = {
                        menu = false
                        onPriceLists()
                    },
                )
            }
        }
    }
}

@Composable
private fun LocationsMapPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(168.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF173652), Color(0xFF0A1D32), Color(0xFF10283F)),
                ),
                shape = RoundedCornerShape(15.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val grid = Color(0xFF3A5872).copy(alpha = 0.36f)
            for (index in 1..6) {
                val x = size.width * index / 7f
                drawLine(
                    color = grid,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            for (index in 1..4) {
                val y = size.height * index / 5f
                drawLine(
                    color = grid,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = Accent,
            )
            Text(
                text = "Karta nije dostupna",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Text(
                text = "Koordinate za kartu nisu objavljene u povezanom katalogu.",
                color = Color(0xFFD4E4F3),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun LocationReferenceCard(
    option: LocationOption,
    selected: Boolean,
    busy: Boolean,
    onSelect: () -> Unit,
    onPriceLists: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = !busy) {
                if (selected) onPriceLists() else onSelect()
            }
            .semantics {
                contentDescription = option.name +
                    (if (selected) ", odabrano, otvori cjenike" else ", odaberi poslovnicu")
            },
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, if (selected) Accent else Border),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 92.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier.size(48.dp)
                    .background(SurfaceRaised, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (option.isWebshop) {
                        Icons.Outlined.Store
                    } else {
                        Icons.Outlined.LocationOn
                    },
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = option.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (option.isWebshop) {
                        "Glavni javni cjenik"
                    } else {
                        "Šifra: " + option.code
                    },
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                    Text(
                        text = "Odabrano",
                        color = Success,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
            TextButton(
                enabled = !busy,
                onClick = if (selected) onPriceLists else onSelect,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (selected) "Cjenici" else "Odaberi",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(name = "09 Poslovnice 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaLocationsPreview() {
    val example = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "2026-10-10",
            publicPage = null,
            archivePage = null,
            productsCsv = "https://primjer.hr/proizvodi.csv",
            servicesCsv = "https://primjer.hr/usluge.csv",
            outlets = listOf(
                Outlet("Rijeka", "rijeka", "", ""),
                Outlet("Opatija", "opatija", "", ""),
                Outlet("Crikvenica", "crikvenica", "", ""),
            ),
        ),
        products = emptyList(),
        services = emptyList(),
        archive = emptyList(),
        warnings = emptyList(),
    )
    SidrenaTheme {
        LocationsPage(
            data = example,
            selectedOutlet = "rijeka",
            busy = false,
            error = null,
            onBack = {},
            onSelect = {},
            onPriceLists = {},
            onRefresh = {},
        )
    }
}
