package com.brendigo.bazna.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source SVG is 1080x1920; the app must not distort its S on other screens. */
class SidrenaSplashGeometryTest {
    @Test
    fun originalCanvasUsesExactBrandSvgLandmarks() {
        val g = splashGeometry(1080f, 1920f)
        assertEquals(1f, g.logoScale, 0.001f)
        assertEquals(16f, g.logoLeft, 0.001f)
        assertEquals(188f, g.logoTop, 0.001f)
        assertEquals(540f, g.centerX, 0.001f)
        assertEquals(1160f, g.headingBaseline, 0.001f)
        assertEquals(1248f, g.firstTaglineBaseline, 0.001f)
        assertEquals(1310f, g.secondTaglineBaseline, 0.001f)
        assertEquals(1720f, g.bylineBaseline, 0.001f)
    }

    @Test
    fun previewSizeScalesSourceExactly() {
        val g = splashGeometry(360f, 640f)
        assertEquals(1f / 3f, g.logoScale, 0.001f)
        assertEquals(16f / 3f, g.logoLeft, 0.001f)
        assertEquals(188f / 3f, g.logoTop, 0.001f)
        assertEquals(1160f / 3f, g.headingBaseline, 0.001f)
        assertEquals(1720f / 3f, g.bylineBaseline, 0.001f)
    }

    @Test
    fun tallPhonePreservesUndistortedLogoAndCenteredTitle() {
        val g = splashGeometry(393f, 852f)
        assertEquals(393f / 1080f, g.logoScale, 0.001f)
        assertEquals(196.5f, g.centerX, 0.001f)
        val left = g.logoLeft + 4f * 69f * g.logoScale
        val right = g.logoLeft + 4f * 193f * g.logoScale
        assertEquals((left + right) / 2f, g.centerX, 0.001f)
        assertTrue(left >= 0f)
        assertTrue(right <= 393f)
        assertTrue(g.bylineBaseline < 852f)
    }

    @Test
    fun compactPhoneDoesNotClipLogoOrFooter() {
        val g = splashGeometry(320f, 568f)
        val left = g.logoLeft + 4f * 69f * g.logoScale
        val right = g.logoLeft + 4f * 193f * g.logoScale
        assertTrue(left >= 0f)
        assertTrue(right <= 320f)
        assertTrue(g.logoTop + 4f * 195f * g.logoScale < g.headingBaseline)
        assertTrue(g.bylineBaseline < 568f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroWidthCannotProduceInvalidLayout() {
        splashGeometry(0f, 640f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroHeightCannotProduceInvalidLayout() {
        splashGeometry(360f, 0f)
    }
}
