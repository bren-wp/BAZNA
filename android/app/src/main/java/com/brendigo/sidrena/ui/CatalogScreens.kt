package com.brendigo.sidrena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.PriceItem

fun formatPrice(raw: String): String {
    if (raw.isBlank()) return "—"
    return com.brendigo.sidrena.data.PriceDisplay.euro(raw)
}

@Composable
fun DetailPage(
    item: PriceItem?,
    data: CatalogSnapshot,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    if (item == null) {
        Column { PageHeading("Detalj stavke"); OutlineAction("Povratak", onBack) }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = SoftBlue)
                Spacer(Modifier.width(8.dp))
                Text("Detalj stavke", color = Color.White,
                    style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(if (item.kind.contains("uslug", true)) Icons.Outlined.Settings
                            else Icons.Outlined.Inventory2, Accent, 72)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            MetaText("Objavljena stavka")
                        }
                    }
                    Separator()
                    DetailRow("Vrsta", item.kind.ifBlank { "—" })
                    DetailRow("SKU", item.sku.ifBlank { "—" })
                    DetailRow("Trenutačna cijena", formatPrice(item.currentPrice))
                    DetailRow("Sidrena cijena", formatPrice(item.referencePrice))
                    DetailRow("Datum sidrene cijene", item.referenceDate.ifBlank { "—" })
                    DetailRow("Dostupnost", item.availability.ifBlank { "—" })
                    DetailRow("Jedinica mjere", item.unit.ifBlank { "—" })
                }
            }
        }
        item {
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(Icons.Outlined.History, SoftBlue)
                        Spacer(Modifier.width(10.dp))
                        Text("Povijest promjena", fontWeight = FontWeight.Bold)
                    }
                    MetaText("Pojedinačni zapisi promjena nisu dostupni kroz javno sučelje povezane stranice.")
                    if (item.referenceDate.isNotBlank() && item.referencePrice.isNotBlank()) {
                        DetailRow("Zabilježena sidrena cijena", formatPrice(item.referencePrice))
                        DetailRow("Datum", item.referenceDate)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineAction("Povratak", onBack, Modifier.weight(1f))
                data.index.publicPage?.let { url ->
                    AccentButton("Javni cjenik", { onOpen(url) }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f),
            color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium)
    }
    Separator()
}
