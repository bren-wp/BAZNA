package com.brendigo.bazna.ui

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * BAZNA rebrand: retains the established 1080 × 1920 layout grid,
 * native gradients and typography while replacing the old S vector with B.
 * New branded reference screenshots are required before a 1:1 claim.
 */
private const val DESIGN_WIDTH = 1080f
private const val DESIGN_HEIGHT = 1920f
private const val SPLASH_DURATION_MS = 2200L

internal data class SplashGeometry(
    val logoScale: Float,
    val logoLeft: Float,
    val logoTop: Float,
    val xScale: Float,
    val yScale: Float,
    val centerX: Float,
    val headingBaseline: Float,
    val firstTaglineBaseline: Float,
    val secondTaglineBaseline: Float,
    val bylineBaseline: Float,
)

/**
 * Exact SVG coordinates on a 1080×1920 screen. On other aspect ratios the
 * letter B and font sizes scale uniformly; vertical landmarks remain anchored
 * to screen height, so the logo will never become stretched.
 */
internal fun splashGeometry(widthPx: Float, heightPx: Float): SplashGeometry {
    require(widthPx > 0f && heightPx > 0f)
    val x = widthPx / DESIGN_WIDTH
    val y = heightPx / DESIGN_HEIGHT
    val scale = min(x, y)
    return SplashGeometry(
        logoScale = scale,
        logoLeft = widthPx / 2f - 524f * scale,
        logoTop = 700f * y - 512f * scale,
        xScale = x,
        yScale = y,
        centerX = widthPx / 2f,
        headingBaseline = 1160f * y,
        firstTaglineBaseline = 1248f * y,
        secondTaglineBaseline = 1310f * y,
        bylineBaseline = 1720f * y,
    )
}

/** BAZNA: custom compact B vector using the original navy/orange palette. */
private fun splashSymbolPath(): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    moveTo(80f, 61f)
    lineTo(135f, 61f)
    quadraticBezierTo(190f, 61f, 190f, 95f)
    quadraticBezierTo(190f, 113f, 173f, 125f)
    quadraticBezierTo(195f, 135f, 195f, 159f)
    quadraticBezierTo(195f, 195f, 135f, 195f)
    lineTo(80f, 195f)
    close()
    moveTo(107f, 88f)
    lineTo(134f, 88f)
    quadraticBezierTo(159f, 88f, 159f, 101f)
    quadraticBezierTo(159f, 115f, 134f, 115f)
    lineTo(107f, 115f)
    close()
    moveTo(107f, 141f)
    lineTo(136f, 141f)
    quadraticBezierTo(164f, 141f, 164f, 155f)
    quadraticBezierTo(164f, 169f, 136f, 169f)
    lineTo(107f, 169f)
    close()
}

@Composable
fun SidrenaSplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Preserve one 2200 ms splash interval across parent recompositions.
    // Use the latest navigation callback without restarting the countdown.
    val latestOnFinished = rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        latestOnFinished.value()
    }
    SidrenaSplashArtwork(modifier)
}

/** Static artwork also supports deterministic Compose screenshot previews. */
@Composable
internal fun SidrenaSplashArtwork(modifier: Modifier = Modifier) {
    val symbol = splashSymbolPath()
    Canvas(
        modifier = modifier.fillMaxSize().semantics {
            contentDescription =
                "BAZNA. Cijene pod kontrolom. Poslovanje s povjerenjem. by Brendigo."
        },
    ) {
        val layout = splashGeometry(size.width, size.height)
        val scale = layout.logoScale

        // Reference SVG: linear-gradient(#07182D, #123457).
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF07182D), Color(0xFF123457)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        // Reference SVG circles at (935,250)/540 and (120,1650)/450.
        withTransform({
            scale(layout.xScale, layout.yScale, pivot = Offset.Zero)
        }) {
            drawCircle(
                color = Color(0xFFFF852C).copy(alpha = 0.055f),
                radius = 540f,
                center = Offset(935f, 250f),
            )
            drawCircle(
                color = Color(0xFF5C8EBB).copy(alpha = 0.055f),
                radius = 450f,
                center = Offset(120f, 1650f),
            )
        }

        // BAZNA B vector on the established splash geometry.
        withTransform({
            translate(left = layout.logoLeft, top = layout.logoTop)
            scale(4f * scale, 4f * scale, pivot = Offset.Zero)
        }) {
            drawPath(
                path = symbol,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFAA55), Color(0xFFFF852C)),
                    start = Offset(69f, 61f),
                    end = Offset(193f, 195f),
                ),
            )
        }

        // Canvas text uses the SVG baseline coordinates, instead of stacking
        // dp-based widgets whose vertical drift grows with screen height.
        val nativeCanvas = drawContext.canvas.nativeCanvas
        drawCenteredText(
            nativeCanvas, "BAZNA", layout.centerX, layout.headingBaseline,
            119f * scale, 0xFFFFFFFF.toInt(), bold = true,
            trackingPx = 10f * scale,
        )
        drawCenteredText(
            nativeCanvas, "Cijene pod kontrolom.",
            layout.centerX, layout.firstTaglineBaseline,
            42f * scale, 0xFFA9BAD1.toInt(),
        )
        drawCenteredText(
            nativeCanvas, "Poslovanje s povjerenjem.",
            layout.centerX, layout.secondTaglineBaseline,
            42f * scale, 0xFFA9BAD1.toInt(),
        )
        drawCenteredText(
            nativeCanvas, "by Brendigo", layout.centerX,
            layout.bylineBaseline, 36f * scale, 0xFFFF852C.toInt(),
        )
    }
}

/** Text is centered with measured tracking to match SVG letter-spacing=10. */
private fun drawCenteredText(
    canvas: AndroidCanvas,
    text: String,
    centerX: Float,
    baseline: Float,
    sizePx: Float,
    colorArgb: Int,
    bold: Boolean = false,
    trackingPx: Float = 0f,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorArgb
        textSize = sizePx
        typeface = Typeface.create(
            "sans-serif",
            if (bold) Typeface.BOLD else Typeface.NORMAL,
        )
    }
    if (trackingPx == 0f) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, centerX, baseline, paint)
        return
    }
    val total = paint.measureText(text) + (text.length - 1) * trackingPx
    var x = centerX - total / 2f
    text.forEach { char ->
        val glyph = char.toString()
        canvas.drawText(glyph, x, baseline, paint)
        x += paint.measureText(glyph) + trackingPx
    }
}

@Preview(name = "BAZNA Splash 01 — 360x640 reference", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
internal fun SidrenaSplashReferencePreview() {
    SidrenaTheme { SidrenaSplashArtwork() }
}

@Preview(name = "BAZNA Splash 01 — tall 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
internal fun SidrenaSplashTallPreview() {
    SidrenaTheme { SidrenaSplashArtwork() }
}
