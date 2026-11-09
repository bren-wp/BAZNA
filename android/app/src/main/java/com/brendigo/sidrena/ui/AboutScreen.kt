package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.sidrena.BuildConfig
import com.brendigo.sidrena.R
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.SiteUrl

/** Published information only. Never substitute the Android version for a plugin version. */
internal data class AboutMetadata(
    val androidVersion: String,
    val pluginVersion: String,
    val publishedAt: String,
    val site: String,
    val rulesRevision: String?,
    val rulesEffectiveFrom: String?,
)

internal fun aboutMetadata(data: CatalogSnapshot, androidVersion: String): AboutMetadata =
    AboutMetadata(
        androidVersion = androidVersion.ifBlank { "Nije dostupna" },
        pluginVersion = data.index.pluginVersion?.trim()
            ?.takeIf { it.isNotEmpty() } ?: "Nije objavljena",
        publishedAt = data.index.publishedAt?.trim()
            ?.takeIf { it.isNotEmpty() } ?: "Datum nije objavljen",
        site = data.site,
        rulesRevision = data.index.legal?.rulesRevision
            ?.takeIf { it.isNotBlank() },
        rulesEffectiveFrom = data.index.legal?.effectiveFrom
            ?.takeIf { it.isNotBlank() },
    )

/** Return the public price-list link only when it passes the same HTTPS-origin rule. */
internal fun aboutPublicPriceListUrl(data: CatalogSnapshot): String? =
    runCatching {
        SiteUrl.parse(data.site).publicLink(data.index.publicPage)
    }.getOrNull()

/**
 * BAZNA Android — screen 12/12, "O aplikaciji".
 *
 * All assets are local. The logo is the same drawable as the splash screen.
 * Content uses real build metadata and the published WordPress public API.
 */
@Composable
fun AboutPage(
    data: CatalogSnapshot,
    onBack: () -> Unit,
    onConnectHelp: () -> Unit,
    onAnchorHelp: () -> Unit,
    onSupport: () -> Unit,
    onOpenPublic: (String) -> Unit,
) {
    val info = aboutMetadata(data, BuildConfig.VERSION_NAME)
    val publicPage = aboutPublicPriceListUrl(data)
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(61.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Natrag na postavke",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "O aplikaciji",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        AboutHero()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AboutVersionCard(
                iconIsAndroid = true,
                title = "Android aplikacija",
                version = info.androidVersion,
                modifier = Modifier.weight(1f),
            )
            AboutVersionCard(
                iconIsAndroid = false,
                title = "WordPress dodatak",
                version = info.pluginVersion,
                modifier = Modifier.weight(1f),
            )
        }

        AboutSectionTitle("POMOĆ I UPUTE")
        AboutGroup {
            AboutActionRow(
                icon = Icons.Outlined.HelpOutline,
                title = "Kako povezati stranicu",
                subtitle = "HTTPS adresa i BAZNA dodatak",
                onClick = onConnectHelp,
            )
            AboutDivider()
            AboutActionRow(
                icon = Icons.Outlined.Description,
                title = "Što su bazne cijene?",
                subtitle = "Referentni podaci i datum primjene",
                onClick = onAnchorHelp,
            )
            if (publicPage != null) {
                AboutDivider()
                AboutActionRow(
                    icon = Icons.Outlined.OpenInNew,
                    title = "Otvori javne cjenike",
                    subtitle = "Objavljeni podaci povezane stranice",
                    onClick = { onOpenPublic(publicPage) },
                )
            }
        }

        AboutSectionTitle("POVEZANA STRANICA")
        AboutGroup {
            AboutReadOnlyRow(
                icon = Icons.Outlined.Public,
                title = "WordPress stranica",
                value = info.site,
            )
            AboutDivider()
            AboutReadOnlyRow(
                icon = Icons.Outlined.Description,
                title = "Posljednja objava",
                value = info.publishedAt,
            )
        }

        if (info.rulesRevision != null || info.rulesEffectiveFrom != null) {
            AboutSectionTitle("OBJAVLJENI PRAVNI PROFIL")
            AboutGroup {
                if (info.rulesRevision != null) {
                    AboutReadOnlyRow(
                        icon = Icons.Outlined.Security,
                        title = "Revizija pravila",
                        value = info.rulesRevision,
                    )
                }
                if (info.rulesEffectiveFrom != null) {
                    if (info.rulesRevision != null) AboutDivider()
                    AboutReadOnlyRow(
                        icon = Icons.Outlined.Info,
                        title = "Datum primjene profila",
                        value = info.rulesEffectiveFrom,
                    )
                }
            }
        }

        if (data.index.legal?.basePriceRegulationStatus == "draft_consultation") {
            AboutSectionTitle("ZAKONSKI STATUS NA 2. 11. 2026.")
            AboutGroup {
                AboutReadOnlyRow(
                    icon = Icons.Outlined.Info,
                    title = "Bazna cijena",
                    value = "Pravilnik je još u savjetovanju do 13. 11. 2026.",
                )
                AboutDivider()
                AboutReadOnlyRow(
                    icon = Icons.Outlined.Security,
                    title = "Provjera povijesnih cijena",
                    value = "Trgovac mora potvrditi cijene i datume izvornom evidencijom.",
                )
                AboutDivider()
                AboutReadOnlyRow(
                    icon = Icons.Outlined.Description,
                    title = "Pravna usklađenost",
                    value = "Nije automatski potvrđena ovim javnim profilom.",
                )
            }
        }

        AboutSectionTitle("PODRŠKA")
        AboutGroup {
            AboutActionRow(
                icon = Icons.Outlined.Email,
                title = "Kontaktiraj podršku",
                subtitle = "Pošalji upit putem aplikacije za e-poštu",
                onClick = onSupport,
            )
        }

        Text(
            text = "BAZNA samo prikazuje javno objavljene podatke " +
                "povezane WordPress stranice. Cijene i datume " +
                "provjerite u izvornim cjenicima.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        Text(
            text = "BAZNA by Brendigo",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AboutHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF153854), Color(0xFF07182D)),
                    ),
                )
                .padding(vertical = 23.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier.size(83.dp)
                        .background(SurfaceRaised, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_sidrena_mark),
                        contentDescription = "Logotip BAZNA – Bazne cijene",
                        modifier = Modifier.size(67.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "BAZNA",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.9.sp,
                    color = Color.White,
                )
                Text(
                    text = "Cijene pod kontrolom.\nPoslovanje s povjerenjem.",
                    color = Color(0xFFD7E4F2),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "by Brendigo",
                    fontSize = 11.sp,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AboutVersionCard(
    iconIsAndroid: Boolean,
    title: String,
    version: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.heightIn(min = 107.dp),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = if (iconIsAndroid) Icons.Outlined.Info else Icons.Outlined.Public,
                contentDescription = null,
                tint = if (iconIsAndroid) Accent else SoftBlue,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = title,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
            )
            Text(
                text = version,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AboutSectionTitle(label: String) {
    Text(
        text = label,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        color = TextMuted,
        letterSpacing = 0.7.sp,
        modifier = Modifier.padding(start = 3.dp, top = 4.dp),
    )
}

@Composable
private fun AboutGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 13.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun AboutDivider() {
    androidx.compose.material3.HorizontalDivider(
        thickness = 1.dp,
        color = Border.copy(alpha = 0.6f),
    )
}

@Composable
private fun AboutActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 76.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AboutIcon(icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                lineHeight = 18.sp,
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 2,
                lineHeight = 16.sp,
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AboutReadOnlyRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 76.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AboutIcon(icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AboutIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.size(43.dp)
            .background(SurfaceRaised, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(23.dp),
            tint = Accent,
        )
    }
}

@Preview(name = "12 O aplikaciji 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaAboutPreview() {
    val data = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "2026-10-10",
            publicPage = "https://primjer.hr/cjenici",
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = emptyList(),
            pluginVersion = "1.5.3",
        ),
        products = emptyList(),
        services = emptyList(),
        archive = emptyList(),
        warnings = emptyList(),
    )
    SidrenaTheme {
        AboutPage(
            data = data,
            onBack = {},
            onConnectHelp = {},
            onAnchorHelp = {},
            onSupport = {},
            onOpenPublic = {},
        )
    }
}

@Preview(name = "12 O aplikaciji compact", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
internal fun SidrenaAboutCompactPreview() {
    SidrenaAboutPreview()
}
