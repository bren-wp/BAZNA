package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brendigo.sidrena.data.CatalogSearch
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.PriceItem

@Composable
fun SearchPage(
    data: CatalogSnapshot,
    onDetails: (PriceItem) -> Unit,
    onBack: () -> Unit,
    onArchive: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableIntStateOf(0) }
    val filtered = CatalogSearch.items(
        if (kind == 1) data.services else data.products, query
    )
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageHeading("Pretraživanje")
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Naziv, SKU ili ID") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Proizvodi", "Usluge", "Arhiva").forEachIndexed { index, label ->
                    FilterChip(
                        selected = kind == index,
                        onClick = { if (index == 2) onArchive() else kind = index },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent, selectedLabelColor = Background)
                    )
                }
            }
            MetaText("Pronađeno " + filtered.size + " rezultata")
        }
        items(filtered) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onDetails(item) },
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = BorderStroke(1.dp, Border),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(if (kind == 1) Icons.Outlined.Settings else Icons.Outlined.Inventory2)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        MetaText(if (kind == 1) "Usluga" else "Proizvod")
                        Text(item.name, fontWeight = FontWeight.Bold)
                        MetaText(formatPrice(item.currentPrice))
                    }
                    Text("›", color = TextMuted, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item {
            if (filtered.isEmpty()) Panel { MetaText("Nema rezultata pretraživanja.") }
            OutlineAction("Natrag", onBack, Modifier.fillMaxWidth())
            Spacer(Modifier.height(15.dp))
        }
    }
}
