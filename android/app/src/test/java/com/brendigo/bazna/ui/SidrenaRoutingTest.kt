package com.brendigo.bazna.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SidrenaRoutingTest {
    @Test fun searchRetainsItsParentUnlessAlreadyInSearchOrDetail() {
        assertTrue(shouldRecordSearchReturn("home", "search"))
        assertTrue(shouldRecordSearchReturn("products", "search"))
        assertFalse(shouldRecordSearchReturn("search", "search"))
        assertFalse(shouldRecordSearchReturn("detail", "search"))
        assertFalse(shouldRecordSearchReturn("settings", "archive"))
    }

    @Test fun backNavigationRestoresExpectedScreen() {
        assertEquals("products", backDestination("detail", "products", "home"))
        assertEquals("search", backDestination("detail", "search", "home"))
        assertEquals("settings", backDestination("about", "products", "home"))
        assertEquals("services", backDestination("search", "products", "services"))
        assertEquals("home", backDestination("archive", "products", "services"))
    }

    @Test fun allBottomTabsAndMappedScreensStayStable() {
        val screens = mapOf(
            "home" to 0, "locations" to 0, "products" to 1,
            "services" to 1, "cjenici" to 1, "detail" to 1,
            "archive" to 2, "settings" to 3, "about" to 3,
        )
        screens.forEach { (page, tab) ->
            assertEquals("Incorrect tab for $page", tab, bottomTabForPage(page, "home"))
        }
        assertEquals(0, bottomTabForPage("search", "home"))
        assertEquals(1, bottomTabForPage("search", "products"))
        assertEquals("home", pageForBottomTab(0))
        assertEquals("cjenici", pageForBottomTab(1))
        assertEquals("archive", pageForBottomTab(2))
        assertEquals("settings", pageForBottomTab(3))
    }
}
