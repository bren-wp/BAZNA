package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * BAZNA 06/12 — Reference screens_concept/06_detalj_proizvoda.png.
 * Three factual cards: item identity, current/reference prices and historical
 * reference data. Mockup's system status bar and DEMO badge are not rendered.
 * Every displayed value comes from the public WordPress CSV, never a demo.
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
    val safePublicPage = runCatching {
        com.brendigo.sidrena.data.SiteUrl.parse(data.site).publicLink(publicPage)
    }.getOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 25.dp),
    ) {
        item {
            DetailReferenceToolbar(
                title = if (view?.isService == true) "Detalji usluge" else "Detalji proizvoda",
                onBack = onBack,
                onOpenPublic = safePublicPage?.let { url -> { onOpen(url) } },
            )
        }
        if (view == null) {
            item {
                DetailReferenceCard {
                    Text(
                        "Stavka više nije dostupna u katalogu.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Vratite se na popis i odaberite stvarno objavljenu stavku.",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                    OutlineAction("Povratak", onBack)
                }
            }
        } else {
            item {
                DetailReferenceHero(
                    site = data.site,
                    imageUrl = item?.imageUrl.orEmpty(),
                    service = view.isService,
                )
            }
            item {
                DetailReferenceCard(background = SurfaceRaised) {
                    Text(
                        view.title.uppercase(java.util.Locale.forLanguageTag("hr-HR")),
                        color = TextMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        detailHeadline(view.title, view.unit),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        lineHeight = 28.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val skuAndAvailability = listOfNotNull(
                        view.sku?.let { "SKU: " + it },
                        view.availability,
                    ).joinToString("  •  ")
                    if (skuAndAvailability.isNotBlank()) {
                        Text(
                            skuAndAvailability,
                            color = if (isPublishedAvailable(view.availability)) Success else TextMuted,
                            fontSize = 13.sp,
                            maxLines = 2,
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            "Trenutačna cijena",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = TextMuted,
                            fontSize = 13.sp,
                        )
                        Text(
                            view.currentPrice,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = if (view.hasCurrentPrice) Accent else TextMuted,
                            fontSize = if (view.hasCurrentPrice) 29.sp else 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(1.dp))
                        Separator()
                        Text(
                            if (view.referencePrice != null)
                                "Sidrena cijena: " + view.referencePrice
                            else "Sidrena cijena nije objavljena",
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 11.dp),
                            color = if (view.referencePrice != null) Color.White else TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (view.referencePrice != null) 18.sp else 14.sp,
                        )
                        view.referenceDate?.let { published ->
                            Text(
                                "Datum sidrene cijene: " + published,
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 11.dp),
                                color = TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
            item {
                DetailReferenceCard {
                    Text(
                        "Podaci o sidrenoj cijeni",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    DetailAttribute(
                        "Referentni datum",
                        view.referenceDate ?: "Datum nije potvrđen",
                    )
                    DetailAttribute(
                        "Izvor",
                        "Javno objavljen cjenik",
                    )
                    if (view.referencePrice != null && view.referenceDate == null) {
                        Text(
                            "Povijesni datum nije objavljen. " +
                                "Trgovac mora potvrditi iznos i datum izvornom evidencijom.",
                            color = Color(0xFFFFC997),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }
            }
            if (view.unitPrice != null || view.saleLabel != null ||
                view.brand != null || view.barcode != null
            ) {
                item {
                    DetailReferenceCard {
                        Text(
                            "Dodatni objavljeni podaci",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        view.unitPrice?.let { DetailAttribute("Cijena za jedinicu mjere", it) }
                        view.saleLabel?.let { DetailAttribute("Poseban oblik prodaje", it) }
                        view.brand?.let { DetailAttribute("Marka", it) }
                        view.barcode?.let { DetailAttribute("Barkod", it) }
                    }
                }
            }
            item {
                Spacer(Modifier.height(23.dp))
                Text(
                    "Informacije su prikazane prema povezanom javnom izvoru. " +
                        "Objava ne potvrđuje zakonsku točnost povijesnih cijena.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/**
 * Screen 06: the 213dp reference hero uses original navy/orange visuals.
 * A real product photo is shown only when published on the connected HTTPS
 * site; services and items without photos use a local vector pictogram.
 */
@Composable
private fun DetailReferenceHero(site: String, imageUrl: String, service: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(213.dp)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF193A5A), Color(0xFF07182D)),
                ),
                RoundedCornerShape(17.dp),
            )
            .border(BorderStroke(1.dp, Border), RoundedCornerShape(17.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (!service && approvedProductImageUrl(site, imageUrl) != null) {
            ProductThumbnail(
                site = site,
                imageUrl = imageUrl,
                modifier = Modifier.size(width = 180.dp, height = 188.dp),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(
                    imageVector = if (service) Icons.Outlined.Build else Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = if (service) "Javno objavljena usluga" else "Fotografija nije objavljena",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/** Availability text is rendered as published, not inferred by substring. */
internal fun isPublishedAvailable(value: String?): Boolean =
    value?.trim()?.lowercase(java.util.Locale.ROOT) in
        setOf("dostupno", "na zalihi", "in stock", "instock")

@Composable
private fun DetailReferenceToolbar(
    title: String,
    onBack: () -> Unit,
    onOpenPublic: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().height(57.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Natrag na popis",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (onOpenPublic != null) {
            Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Javni cjenik", tint = TextMuted)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Otvori javni cjenik") },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
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

@Composable
private fun DetailReferenceCard(
    background: Color = Surface,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** Do not append the public unit twice when it is already in the product name. */
internal fun detailHeadline(name: String, unit: String?): String {
    val clean = name.trim()
    val u = unit?.trim().orEmpty()
    return if (u.isEmpty() || clean.endsWith(u, ignoreCase = true)) clean
        else clean + " " + u
}

@Composable
private fun DetailAttribute(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
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

@Preview(name = "06 BAZNA detalj — 393×852", widthDp = 393, heightDp = 852, showBackground = true)
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
