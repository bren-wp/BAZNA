package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.PriceItem

/** Currency formatting follows the published public WordPress values. */
fun formatPrice(raw: String): String {
    if (raw.isBlank()) return "—"
    return com.brendigo.sidrena.data.PriceDisplay.euro(raw)
}

/**
 * 06/12: item details based on the sixth SIDRENA branded screen.
 *
 * The public WordPress CSV can contain an optional product image, actual unit
 * price, special sale label and historical reference date. Missing values stay unknown.
 */
@Composable
fun DetailPage(
    item: PriceItem?,
    data: CatalogSnapshot,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val view = item?.let(::detailPresentation)
    val publicPage = data.index.publicPage

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            DetailToolbar(
                title = if (view?.isService == true) "Detalj usluge" else "Detalj artikla",
                hasPublicLink = !publicPage.isNullOrBlank(),
                onBack = onBack,
                onOpenPublic = { publicPage?.let(onOpen) },
            )
        }

        if (view == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Border),
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        Text("Stavka nije dostupna.", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Vratite se na popis i ponovno odaberite artikl.",
                            color = TextMuted, fontSize = 14.sp,
                        )
                        OutlineAction("Povratak na popis", onBack, Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            item {
                DetailHero(isService = view.isService, site = data.site, imageUrl = item?.imageUrl.orEmpty())
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = view.title,
                        color = Color.White,
                        fontSize = 23.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (view.unit != null) {
                        Text(
                            text = view.unit,
                            color = TextMuted,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
            item {
                DetailPricePanel(view = view)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        Text(
                            "Podaci o " + (if (view.isService) "usluzi" else "artiklu"),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        view.sku?.let { DetailAttribute("Šifra", it) }
                        view.availability?.let { DetailAttribute("Dostupnost", it) }
                        view.unit?.let { DetailAttribute("Jedinica mjere", it) }
                        view.brand?.let { DetailAttribute("Marka", it) }
                        view.barcode?.let { DetailAttribute("Barkod", it) }
                        if (view.sku == null && view.availability == null && view.unit == null) {
                            Text(
                                "Dodatni podaci nisu objavljeni.",
                                color = TextMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            if (!publicPage.isNullOrBlank()) {
                item {
                    AccentButton(
                        label = "Otvori javni cjenik",
                        onClick = { onOpen(publicPage) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailToolbar(
    title: String,
    hasPublicLink: Boolean,
    onBack: () -> Unit,
    onOpenPublic: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().height(57.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Povratak na popis",
                tint = Color.White,
                modifier = Modifier.size(25.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (hasPublicLink) {
            Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Opcije artikla",
                        tint = Color.White,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Javni cjenik") },
                        leadingIcon = {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                        },
                        onClick = {
                            expanded = false
                            onOpenPublic()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Prefer a genuine, same-origin WordPress product thumbnail when available.
 * Otherwise render the native placeholder without invented imagery.
 */
@Composable
private fun DetailHero(isService: Boolean, site: String, imageUrl: String) {
    if (!isService && approvedProductImageUrl(site, imageUrl) != null) {
        ProductThumbnail(
            site = site,
            imageUrl = imageUrl,
            modifier = Modifier.fillMaxWidth().height(213.dp),
        )
        return
    }
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(213.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF1F4967), Color(0xFF0B2139), Color(0xFF12324D)),
                ),
                shape = RoundedCornerShape(17.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val diagonal = Path().apply {
                moveTo(w * 0.67f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.50f)
                close()
            }
            drawPath(diagonal, Color(0xFF294E69).copy(alpha = 0.54f))
            drawCircle(
                color = Accent.copy(alpha = 0.10f),
                radius = size.minDimension * 0.31f,
                center = Offset(w * 0.50f, h * 0.46f),
            )
            drawCircle(
                color = Accent.copy(alpha = 0.35f),
                radius = size.minDimension * 0.23f,
                center = Offset(w * 0.50f, h * 0.46f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isService) Icons.Outlined.Settings
                else Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = Color(0xFFFFA45D),
                modifier = Modifier.size(65.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Fotografija nije objavljena",
                color = Color(0xFFD4E4F3),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun DetailPricePanel(view: DetailPresentation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Trenutačna cijena",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = view.currentPrice,
                    color = if (view.hasCurrentPrice) Accent else TextMuted,
                    fontSize = if (view.hasCurrentPrice) 29.sp else 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                view.availability?.let { availability ->
                    Text(
                        text = availability,
                        color = if (
                            availability.equals("Dostupno", ignoreCase = true)
                        ) Success else TextMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 2,
                    )
                }
            }
            view.saleLabel?.let { sale ->
                Text("Poseban oblik prodaje: " + sale,
                    color = SoftBlue, fontSize = 12.sp)
            }
            view.unitPrice?.let { unitPrice ->
                Text("Cijena za jedinicu mjere: " + unitPrice,
                    color = TextMuted, fontSize = 12.sp)
            }
            if (view.referencePrice != null) {
                Spacer(Modifier.height(3.dp))
                Separator()
                Spacer(Modifier.height(3.dp))
                Text("Sidrena cijena", color = TextMuted, fontSize = 13.sp)
                Text(
                    text = view.referencePrice,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (view.referenceDate != null) {
                    Text(
                        text = "Datum sidrene cijene: " + view.referenceDate,
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                } else {
                    Text(
                        text = "Referentni datum nije objavljen. " +
                            "Povijesnu cijenu i datum mora potvrditi trgovac.",
                        color = Color(0xFFFFC997),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            } else {
                Text(
                    "Sidrena cijena nije objavljena.",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun DetailAttribute(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.weight(0.9f),
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.1f),
        )
    }
}

internal data class DetailPresentation(
    val title: String,
    val isService: Boolean,
    val unit: String?,
    val currentPrice: String,
    val hasCurrentPrice: Boolean,
    val referencePrice: String?,
    val referenceDate: String?,
    val sku: String?,
    val availability: String?,
    val unitPrice: String?,
    val saleLabel: String?,
    val barcode: String?,
    val brand: String?,
)

/** A pure mapping suitable for JVM tests without the Android runtime. */
internal fun detailPresentation(item: PriceItem): DetailPresentation {
    val hasCurrent = item.currentPrice.isNotBlank()
    val hasReference = item.referencePrice.isNotBlank()
    return DetailPresentation(
        title = item.name.ifBlank { "Bez naziva" },
        isService = item.kind.contains("uslug", ignoreCase = true) ||
            item.kind.contains("service", ignoreCase = true),
        unit = item.unit.takeIf { it.isNotBlank() },
        currentPrice = if (hasCurrent) formatPrice(item.currentPrice) else "Cijena nije objavljena",
        hasCurrentPrice = hasCurrent,
        referencePrice = item.referencePrice.takeIf { hasReference }?.let(::formatPrice),
        referenceDate = item.referenceDate.takeIf { hasReference && it.isNotBlank() },
        sku = item.sku.takeIf { it.isNotBlank() },
        availability = item.availability.takeIf { it.isNotBlank() },
        unitPrice = item.unitPrice.takeIf { it.isNotBlank() }?.let(::formatPrice),
        saleLabel = item.saleLabel.takeIf { it.isNotBlank() },
        barcode = item.barcode.takeIf { it.isNotBlank() },
        brand = item.brand.takeIf { it.isNotBlank() },
    )
}

/** Save only public CSV fields in the Android saved-instance-state Bundle. */
internal fun saveDetailItem(item: PriceItem): ArrayList<String> = arrayListOf(
    item.id,
    item.name,
    item.kind,
    item.currentPrice,
    item.referencePrice,
    item.referenceDate,
    item.sku,
    item.availability,
    item.unit,
    item.imageUrl,
    item.unitPrice,
    item.saleLabel,
    item.barcode,
    item.brand,
)

/** Restores a selected detail after activity recreation; no network changes. */
internal fun restoreDetailItem(fields: List<String>): PriceItem? {
    if (fields.size != 9 && fields.size != 10 && fields.size != 14) return null
    return PriceItem(
        id = fields[0],
        name = fields[1],
        kind = fields[2],
        currentPrice = fields[3],
        referencePrice = fields[4],
        referenceDate = fields[5],
        sku = fields[6],
        availability = fields[7],
        unit = fields[8],
        imageUrl = fields.getOrElse(9) { "" },
        unitPrice = fields.getOrElse(10) { "" },
        saleLabel = fields.getOrElse(11) { "" },
        barcode = fields.getOrElse(12) { "" },
        brand = fields.getOrElse(13) { "" },
    )
}

@Preview(name = "06 SIDRENA detalj — 393×852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaDetailPreview() {
    val demoItem = PriceItem(
        id = "MU-001",
        name = "Maslinovo ulje",
        kind = "proizvod",
        currentPrice = "5,99",
        referencePrice = "8,49",
        referenceDate = "2026-10-01",
        sku = "MU-001",
        availability = "Dostupno",
        unit = "1 L",
    )
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
        products = listOf(demoItem),
        services = emptyList(),
        archive = emptyList(),
        warnings = emptyList(),
    )
    SidrenaTheme {
        DetailPage(
            item = demoItem,
            data = demo,
            onBack = {},
            onOpen = {},
        )
    }
}
