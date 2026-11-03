package com.brendigo.sidrena.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Deterministic reference spacing with a scrollable compact layout. */
internal data class ConnectionSpacing(
    val top: Dp,
    val intro: Dp,
    val field: Dp,
    val primary: Dp,
    val notice: Dp,
    val footer: Dp,
)

internal fun connectionSpacing(heightDp: Float): ConnectionSpacing =
    if (heightDp >= 760f) {
        ConnectionSpacing(24.dp, 48.dp, 60.dp, 42.dp, 32.dp, 124.dp)
    } else {
        ConnectionSpacing(14.dp, 24.dp, 30.dp, 20.dp, 22.dp, 45.dp)
    }

/** On-input guard. Actual URL and origin verification stay in SiteUrl.parse. */
internal fun acceptedSiteInput(value: String): String? =
    value.takeIf {
        it.length <= 2048 && !it.contains('\n') && !it.contains('\r')
    }

/**
 * BAZNA screen 02/12 — connects to a real public WordPress website.
 *
 * Reference: screens_concept/02_povezivanje.png (1080 × 1920).
 * Demo badges, sample addresses as real values and simulated status bar icons
 * are intentionally excluded. The HTTPS example is a placeholder only.
 */
@Composable
fun ConnectionPage(
    input: String,
    busy: Boolean,
    error: String?,
    canCancel: Boolean,
    onInput: (String) -> Unit,
    onConnect: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Background),
    ) {
        // Snapshot viewport constraints here. Nested Column/Box scopes cannot
        // implicitly resolve BoxWithConstraintsScope.maxWidth/maxHeight.
        val viewportWidthDp = maxWidth.value
        val viewportHeightDp = maxHeight.value
        val spacing = connectionSpacing(viewportHeightDp)
        Column(
            modifier = Modifier.fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(spacing.top))
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.offset(x = (-13).dp).size(48.dp),
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = if (canCancel) "Povratak" else "Zatvori aplikaciju",
                        tint = Accent,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Text(
                    "Poveži WordPress stranicu",
                    color = Color.White,
                    fontSize = if (viewportWidthDp >= 360f) 27.sp else 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = if (viewportWidthDp >= 360f) 30.sp else 25.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 48.dp),
                )
            }

            Spacer(Modifier.height(spacing.intro))
            // Screen 02 reference: raised blue connectivity medallion, not a
            // downloaded image or an externally loaded icon asset.
            val linkSize = if (viewportHeightDp >= 760f) 118.dp else 104.dp
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .size(linkSize)
                    .shadow(9.dp, CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF204B7C), Color(0xFF122B4D)),
                        ),
                        CircleShape,
                    )
                    .border(BorderStroke(1.dp, Color(0xFF41678B)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Link,
                    contentDescription = null,
                    tint = SoftBlue,
                    modifier = Modifier.size(if (viewportHeightDp >= 760f) 52.dp else 46.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Svi javni cjenici na jednom mjestu",
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "Poveži svoju web-stranicu za pristup " +
                    "objavljenim proizvodima, uslugama i arhivi.",
                color = TextMuted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )

            Spacer(Modifier.height(spacing.field))
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                border = BorderStroke(1.dp, Border),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "HTTPS adresa web-stranice",
                        fontSize = 13.sp,
                        color = TextMuted,
                    )
                    BasicTextField(
                        value = input,
                        onValueChange = { value ->
                            acceptedSiteInput(value)?.let(onInput)
                        },
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = 25.dp)
                            .semantics {
                                contentDescription = "HTTPS adresa WordPress stranice"
                            },
                        enabled = !busy,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        cursorBrush = SolidColor(Accent),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboard?.hide()
                                if (!busy) onConnect(input.trim())
                            },
                        ),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (input.isEmpty()) {
                                    Text(
                                        text = "https://primjer-trgovina.hr",
                                        color = TextMuted,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(spacing.primary))
            Button(
                onClick = {
                    keyboard?.hide()
                    // Empty values are validated by SiteUrl.parse and produce
                    // a visible error; never connect to the demo placeholder.
                    onConnect(input.trim())
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Background,
                    disabledContainerColor = Accent.copy(alpha = 0.6f),
                    disabledContentColor = Background,
                ),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Background,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = if (busy) "Povezivanje…" else "Poveži stranicu",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = BorderStroke(1.dp, Color(0xFFFF737D)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFFFC6A0),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(spacing.notice))
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = BorderStroke(1.dp, Border),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Sigurna veza",
                            color = Success,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                    }
                    Text(
                        text = "Koristi se javno dostupni WordPress " +
                            "sadržaj bez uređivanja cijena.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }

            Spacer(Modifier.height(spacing.footer))
            Text(
                text = "BAZNA • by Brendigo",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(35.dp))
        }
    }
}

@Preview(name = "02 BAZNA Connection 360x640", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
internal fun SidrenaConnectReferencePreview() {
    SidrenaTheme {
        ConnectionPage(
            input = "",
            busy = false,
            error = null,
            canCancel = false,
            onInput = {},
            onConnect = {},
            onCancel = {},
        )
    }
}

@Preview(name = "02 BAZNA Connection tall 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaConnectTallPreview() {
    SidrenaTheme {
        ConnectionPage(
            input = "https://trgovina.example.hr",
            busy = false,
            error = null,
            canCancel = true,
            onInput = {},
            onConnect = {},
            onCancel = {},
        )
    }
}
