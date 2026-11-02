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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.sidrena.R
import com.brendigo.sidrena.data.ArchiveEntry
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import java.time.LocalDate
import java.util.Locale
import java.text.NumberFormat

private val HomeLocationTint = Color(0xFFFF687A)
private val HomeArchiveTint = Color(0xFFFF8053)
private val HomeConnectedTint = Color(0xFF52D9A3)

/**
 * SIDRENA screen 03/12 — public dashboard.
 *
 * Follows the approved layout: wordmark/search, connection status,
 * 2×2 catalog cards, outlet card, and latest real archived publications.
 * Every count and date derives from [CatalogSnapshot], never a screenshot
 * placeholder. All navigation remains in the existing SidrenaApp.
 */
@Composable
fun HomePage(
    data: CatalogSnapshot,
    busy: Boolean,
    error: String?,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onOpen: (String) -> Unit,
    selectedOutlet: String = "",
) {
    var legalExpanded by rememberSaveable { mutableStateOf(false) }
    val recent = remember(data, selectedOutlet) { homeRecentPublications(data, selectedOutlet) }
    val metrics = remember(data, selectedOutlet) { homeMetrics(data, selectedOutlet) }
    val countFormat = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("hr-HR")) }
    val count: (Int) -> String = { countFormat.format(it) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeTopBar(onSearch = { onNavigate("search") })

        HomeConnectedCard(
            site = data.site,
            publishedAt = data.index.publishedAt,
            busy = busy,
            publicationPending = homeCatalogPending(data, selectedOutlet),
            changesPending = data.index.pendingPublication == true,
            onRefresh = onRefresh,
            onEdit = onEdit,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeMetricCard(
                title = "Proizvodi",
                value = metrics[0].count,
                icon = Icons.Outlined.LocalOffer,
                tint = Accent,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("products") },
            )
            HomeMetricCard(
                title = "Usluge",
                value = metrics[1].count,
                icon = Icons.Outlined.Settings,
                tint = SoftBlue,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("services") },
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeMetricCard(
                title = "Cjenici",
                value = metrics[2].count,
                icon = Icons.Outlined.Description,
                tint = SoftBlue,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("cjenici") },
            )
            HomeMetricCard(
                title = "Arhiva",
                value = metrics[3].count,
                icon = Icons.Outlined.Archive,
                tint = HomeArchiveTint,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("archive") },
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth()
                .clickable(role = Role.Button) { onNavigate("locations") },
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Border),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = HomeLocationTint,
                    modifier = Modifier.size(38.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Poslovnice",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        count(data.index.outlets.size),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Nedavno ažurirano",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 3.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Border),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                when {
                    recent.isNotEmpty() -> recent.forEach { entry ->
                        HomeRecentRow(
                            title = entry.title,
                            subtitle = listOfNotNull(
                                entry.format,
                                entry.date?.takeIf(String::isNotBlank),
                            ).joinToString(" · "),
                            icon = Icons.Outlined.Description,
                            iconTint = if (entry.format.equals("xml", true)) Accent else SoftBlue,
                            onClick = { onOpen(entry.url) },
                        )
                    }
                    else -> Text(
                        "Nema objavljenih datoteka na povezanoj stranici.",
                        modifier = Modifier.padding(16.dp),
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Accent,
                )
                Spacer(Modifier.width(8.dp))
                Text("Osvježavanje podataka…", color = TextMuted, fontSize = 13.sp)
            }
        }

        if (!error.isNullOrBlank()) {
            Text(error, color = Color(0xFFFFC997), fontSize = 13.sp)
        }
        data.warnings.take(3).forEach { warning ->
            Text(warning, color = Color(0xFFFFC997), fontSize = 13.sp)
        }

        TextButton(
            onClick = { legalExpanded = !legalExpanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (legalExpanded) "Sakrij pravne informacije" else "Pravne informacije")
        }
        if (legalExpanded) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = BorderStroke(1.dp, Border),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("Sidrene cijene — pravni okvir", fontWeight = FontWeight.Bold)
                    val legal = data.index.legal
                    if (legal != null) {
                        Text("Početak primjene: ${legal.effectiveFrom}", color = TextMuted, fontSize = 13.sp)
                        Text(
                            "Arhiva: najmanje ${legal.archiveMinDays} dana · Dnevna objava do ${legal.dailyDeadline}",
                            color = TextMuted, fontSize = 13.sp,
                        )
                    } else {
                        Text(
                            "Pravni profil nije dostupan na povezanoj stranici.",
                            color = TextMuted, fontSize = 13.sp,
                        )
                    }
                    Text(
                        "Prikaz javnih podataka ne potvrđuje zakonsku usklađenost trgovca.",
                        color = TextMuted, fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_sidrena_mark),
            contentDescription = null,
            modifier = Modifier.size(35.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "SIDRENA",
            color = Color.White,
            fontSize = 22.sp,
            letterSpacing = 0.2.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Pretraži proizvode i usluge",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun HomeConnectedCard(
    site: String,
    publishedAt: String?,
    busy: Boolean,
    publicationPending: Boolean,
    changesPending: Boolean,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // Pending local changes must not be presented with a green success check.
    val pending = publicationPending || changesPending

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).background(
                        if (pending) Accent.copy(alpha = 0.16f)
                        else HomeConnectedTint,
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (pending) Icons.Outlined.Info
                        else Icons.Outlined.Check,
                        contentDescription = null,
                        tint = if (pending) Accent else Background,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (changesPending) "Promjene čekaju objavu"
                    else if (publicationPending) "Cjenici čekaju objavu" else "Povezano",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "Opcije povezane stranice",
                            tint = TextMuted,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Osvježi podatke") },
                            leadingIcon = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                            enabled = !busy,
                            onClick = {
                                menuExpanded = false
                                onRefresh()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Promijeni stranicu") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                    }
                }
            }
            Text(
                text = homeSiteLabel(site),
                modifier = Modifier.padding(start = 40.dp, end = 9.dp),
                color = TextMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (changesPending)
                    "Objavljeni CSV može biti zastario. Objavite promjene u WordPressu i osvježite."
                else if (publicationPending)
                    "Najprije objavite CSV cjenike u WordPress dodatku."
                else "Zadnje ažuriranje: " +
                    (publishedAt?.takeIf { it.isNotBlank() } ?: "Datum nije dostupan"),
                color = Color(0xFFD8E4F0),
                fontSize = 12.sp,
                maxLines = 2,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun HomeMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.heightIn(min = 132.dp)
            .clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(32.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    value,
                    color = Color.White,
                    fontSize = if (value.length > 6) 19.sp else 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeRecentRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .height(62.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp)
                .background(SurfaceRaised, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                tint = iconTint,
                contentDescription = null,
                modifier = Modifier.size(23.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle.ifBlank { "Podaci o datumu nisu dostupni" },
                color = TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun homeArchiveLabel(entry: ArchiveEntry): String = when {
    entry.kind.contains("uslug", ignoreCase = true) -> "Cjenik usluga"
    entry.kind.contains("proizvod", ignoreCase = true) -> "Cjenik proizvoda"
    else -> "Arhivirani cjenik"
}

/** Preserve server order when an archive date is absent or unparseable. */
internal fun homeRecentArchive(entries: List<ArchiveEntry>): List<ArchiveEntry> =
    entries.distinctBy { it.url }
        .sortedByDescending { homeDateKey(it.date) }
        .take(3)

/** Handles ISO strings and Croatian dd. MM. yyyy. dates without locale-dependent parsing. */
internal fun homeDateKey(raw: String): Long {
    val iso = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(raw)
    val hr = Regex("""(\d{1,2})\.\s*(\d{1,2})\.\s*(\d{4})""").find(raw)
    return runCatching {
        when {
            iso != null -> LocalDate.of(
                iso.groupValues[1].toInt(),
                iso.groupValues[2].toInt(),
                iso.groupValues[3].toInt(),
            ).toEpochDay()
            hr != null -> LocalDate.of(
                hr.groupValues[3].toInt(),
                hr.groupValues[2].toInt(),
                hr.groupValues[1].toInt(),
            ).toEpochDay()
            else -> Long.MIN_VALUE
        }
    }.getOrDefault(Long.MIN_VALUE)
}

@Preview(name = "03 SIDRENA Početna 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaHomePreview() {
    val site = CatalogSnapshot(
        site = "https://trgovinahrvatska.hr",
        index = CatalogIndex(
            publishedAt = "10. 10. 2026. 14:20",
            publicPage = "https://trgovinahrvatska.hr/cjenici",
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = emptyList(),
        ),
        products = emptyList(),
        services = emptyList(),
        archive = listOf(
            ArchiveEntry("10. 10. 2026.", "proizvodi", "", "csv", "https://trgovinahrvatska.hr/1.csv"),
            ArchiveEntry("09. 10. 2026.", "usluge", "", "xml", "https://trgovinahrvatska.hr/2.xml"),
        ),
        warnings = emptyList(),
    )
    SidrenaTheme {
        HomePage(
            data = site,
            busy = false,
            error = null,
            onNavigate = {},
            onRefresh = {},
            onEdit = {},
            onOpen = {},
            selectedOutlet = "",
        )
    }
}
