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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brendigo.sidrena.BuildConfig

@Composable
fun AboutPage(
    onBack: () -> Unit,
    onConnectHelp: () -> Unit,
    onAnchorHelp: () -> Unit,
    onSupport: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = SoftBlue)
            Spacer(Modifier.width(8.dp))
            Text("Postavke", color = SoftBlue)
        }
        PageHeading("O aplikaciji", "SIDRENA by Brendigo — cjenici na jednom mjestu.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Panel(Modifier.weight(1f)) {
                Column {
                    AppIcon(Icons.Outlined.Info)
                    Spacer(Modifier.height(8.dp))
                    Text("Verzija aplikacije", fontWeight = FontWeight.Bold)
                    MetaText(BuildConfig.VERSION_NAME)
                }
            }
            Panel(Modifier.weight(1f)) {
                Column {
                    AppIcon(Icons.Outlined.Public, SoftBlue)
                    Spacer(Modifier.height(8.dp))
                    Text("WordPress dodatak", fontWeight = FontWeight.Bold)
                    MetaText("SIDRENA")
                }
            }
        }
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Pomoć i upute", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                RowLink(Icons.Outlined.Link, "Kako povezati stranicu",
                    "Povezivanje WordPress stranice", onConnectHelp)
                Separator()
                RowLink(Icons.Outlined.Description, "Što su sidrene cijene?",
                    "Referentni podaci i datum cijene", onAnchorHelp, Accent)
            }
        }
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(Icons.Outlined.Email, Accent, 52)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Trebate dodatnu pomoć?", fontWeight = FontWeight.Bold)
                        MetaText("Kontaktirajte podršku za pitanja i sugestije.")
                    }
                }
                AccentButton("Kontaktiraj podršku", onSupport, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("SIDRENA by Brendigo", modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MetaText("brendigo.com")
        Spacer(Modifier.height(18.dp))
    }
}
