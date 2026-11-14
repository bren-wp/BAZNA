package com.brendigo.bazna.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deterministic geometry tests across compact phones, standard phones, tablets and landscape. */
class AdaptiveSizingTest {
    @Test fun contentAlwaysFitsAllRepresentativeWidths() {
        val widths = listOf(240, 280, 320, 360, 393, 411, 480, 600, 720, 840, 1024, 1280, 1920)
        widths.forEach { width ->
            val content = AdaptiveSizing.contentWidthDp(width)
            val connection = AdaptiveSizing.contentWidthDp(width, connecting = true)
            assertTrue("App content exceeds ${width}dp", content in 1..width)
            assertTrue("Connect exceeds ${width}dp", connection in 1..width)
            assertTrue("Tablet content exceeds 840dp", content <= 840)
            assertTrue("Tablet connection exceeds 560dp", connection <= 560)
        }
    }

    @Test fun compactPhonesUseFourAccessibleIconTabs() {
        for (width in listOf(240, 280, 320, 359)) {
            assertTrue(AdaptiveSizing.iconOnlyNavigation(width))
        }
        for (width in listOf(360, 393, 411, 600, 840, 1280)) {
            assertFalse(AdaptiveSizing.iconOnlyNavigation(width))
        }
    }

    @Test fun largeFontUsesAccessibleIconOnlyNavigationWhenLabelsCannotFit() {
        assertTrue(AdaptiveSizing.iconOnlyNavigation(360, fontScale = 1.5f))
        assertTrue(AdaptiveSizing.iconOnlyNavigation(411, fontScale = 2f))
        assertFalse(AdaptiveSizing.iconOnlyNavigation(480, fontScale = 2f))
        assertFalse(AdaptiveSizing.iconOnlyNavigation(393, fontScale = 1f))
    }

    @Test fun sourceArtworkRetainsUnscaledPhoneWidths() {
        for (width in listOf(320, 360, 393, 411, 480, 600)) {
            assertEquals(width, AdaptiveSizing.contentWidthDp(width))
        }
        assertEquals(840, AdaptiveSizing.contentWidthDp(1280))
        assertEquals(560, AdaptiveSizing.contentWidthDp(1280, connecting = true))
    }
}
