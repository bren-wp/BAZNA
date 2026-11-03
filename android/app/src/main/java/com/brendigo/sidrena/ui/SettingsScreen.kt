package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.sidrena.BuildConfig
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.Outlet

/** Render real publication metadata; do not invent dates, versions or outlets. */
internal data class SettingsSummary(
    val site: String,
    val outletName: String,
    val publishedAt: String,
    val pluginVersion: String,
    val rulesRevision: String,
)

internal fun settingsSummary(
    data: CatalogSnapshot,
    selectedOutlet: String,
): SettingsSummary {
    val name = if (selectedOutlet.isBlank()) {
        "Webshop"
    } else {
        data.index.outlets.firstOrNull { it.code == selectedOutlet }
            ?.name?.takeIf(String::isNotBlank)
            ?: "Poslovnica nije dostupna"
    }
    return SettingsSummary(
        site = data.site,
        outletName = name,
        publishedAt = data.index.publishedAt
            ?.takeIf(String::isNotBlank) ?: "Datum nije objavljen",
        pluginVersion = data.index.pluginVersion
            ?.takeIf(String::isNotBlank) ?: "Nije objavljena",
        rulesRevision = data.index.legal?.rulesRevision
            ?.takeIf(String::isNotBlank) ?: "Nije dostupan",
    )
}

/**
 * BAZNA screen 11/12 — settings.
 *
 * The dark theme and Croatian text are current fixed app capabilities,
 * deliberately presented as information rather than inactive switches.
 * The linked site, refresh, outlets, help, about and disconnect are real.
 */
@Composable
fun SettingsPage(
    data: CatalogSnapshot,
    selectedOutlet: String,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onEditSite: () -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onOutlets: () -> Unit,
    onAbout: () -> Unit,
    onHelp: () -> Unit,
) {
    val summary = settingsSummary(data, selectedOutlet)
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsToolbar(onBack)
        Text(
            text = "Upravljanje povezanom stranicom i aplikacijom.",
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        SettingsSectionTitle("POVEZANA STRANICA")
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Border),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SettingsIcon(Icons.Outlined.Public, tint = SoftBlue)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "WordPress stranica",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            summary.site,
                            color = Color(0xFFD9E6F2),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(7.dp)
                                    .background(Success, CircleShape),
                            )
                            Text(
                                if (busy) "Osvježavanje…" else "Povezano",
                                color = Success,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                SettingsDivider()
                Text(
                    "Zadnja objava: " + summary.publishedAt,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (busy) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            color = Accent,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            "Provjera javnih podataka…",
                            color = TextMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = Color(0xFFFFC997),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onRefresh,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Background,
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Osvježi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        onClick = onEditSite,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Border),
                    ) {
                        Text(
                            "Promijeni URL",
                            color = if (busy) TextMuted else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        SettingsSectionTitle("POSLOVNICA I CJENICI")
        SettingsGroup {
            SettingsEntry(
                icon = Icons.Outlined.Store,
                title = "Odabrana poslovnica",
                subtitle = summary.outletName,
                onClick = onOutlets,
            )
        }

        SettingsSectionTitle("PRIKAZ")
        SettingsGroup {
            SettingsEntry(
                icon = Icons.Outlined.Settings,
                title = "Tema aplikacije",
                subtitle = "Tamna tema",
                onClick = null,
            )
            SettingsDivider()
            SettingsEntry(
                icon = Icons.Outlined.Language,
                title = "Jezik aplikacije",
                subtitle = "Hrvatski",
                onClick = null,
            )
        }

        SettingsSectionTitle("INFORMACIJE I PODRŠKA")
        SettingsGroup {
            SettingsEntry(
                icon = Icons.Outlined.HelpOutline,
                title = "Pomoć i vodiči",
                subtitle = "Upute za povezivanje i sidrene cijene",
                onClick = onHelp,
            )
            SettingsDivider()
            SettingsEntry(
                icon = Icons.Outlined.Info,
                title = "O aplikaciji",
                subtitle = "BAZNA " + BuildConfig.VERSION_NAME + " · by Brendigo",
                onClick = onAbout,
            )
        }

        SettingsGroup {
            SettingsEntry(
                icon = Icons.Outlined.Description,
                title = "WordPress dodatak",
                subtitle = "Verzija: " + summary.pluginVersion,
                onClick = null,
            )
            SettingsDivider()
            SettingsEntry(
                icon = Icons.Outlined.Link,
                title = "Pravni profil",
                subtitle = summary.rulesRevision,
                onClick = null,
            )
        }

        TextButton(
            onClick = onDisconnect,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Icon(
                Icons.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = if (busy) TextMuted else Color(0xFFFFAD9B),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                "Odspoji stranicu",
                color = if (busy) TextMuted else Color(0xFFFFAD9B),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        Text(
            text = "Odspajanje uklanja spremljenu adresu samo na ovom uređaju. " +
                "Podaci na WordPress stranici ostaju nepromijenjeni.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun SettingsToolbar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(61.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Natrag na početnu",
                modifier = Modifier.size(25.dp),
                tint = Color.White,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Postavke",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
    }
}

@Composable
private fun SettingsSectionTitle(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(start = 3.dp, top = 5.dp),
        color = TextMuted,
        fontSize = 11.sp,
        letterSpacing = 0.75.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = modifier
            .height(76.dp)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIcon(icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = TextMuted,
            )
        }
    }
}

@Composable
private fun SettingsIcon(
    icon: ImageVector,
    tint: Color = Accent,
) {
    Box(
        modifier = Modifier.size(43.dp)
            .background(SurfaceRaised, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = Border.copy(alpha = 0.65f),
    )
}

@Preview(name = "11 BAZNA Postavke 393×852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaSettingsPreview() {
    val data = CatalogSnapshot(
        site = "https://trgovina.example.hr",
        index = CatalogIndex(
            publishedAt = "10. 10. 2026.",
            publicPage = "https://trgovina.example.hr/cjenici",
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = listOf(
                Outlet("Rijeka", "rijeka", "", ""),
                Outlet("Opatija", "opatija", "", ""),
            ),
            pluginVersion = "1.5.3",
        ),
        products = emptyList(),
        services = emptyList(),
        archive = emptyList(),
        warnings = emptyList(),
    )
    SidrenaTheme {
        SettingsPage(
            data = data,
            selectedOutlet = "rijeka",
            busy = false,
            error = null,
            onBack = {},
            onEditSite = {},
            onRefresh = {},
            onDisconnect = {},
            onOutlets = {},
            onAbout = {},
            onHelp = {},
        )
    }
}

@Preview(name = "11 BAZNA Postavke kompaktno", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
internal fun SidrenaSettingsCompactPreview() {
    SidrenaSettingsPreview()
}
