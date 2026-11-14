package com.brendigo.bazna.ui

import com.brendigo.bazna.data.Outlet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationsScreenPresentationTest {
    private val outlets = listOf(
        Outlet(
            name = "Rijeka Centar",
            code = "rijeka-centar",
            address = "Korzo 12, Rijeka",
            productCsv = "https://primjer.hr/rijeka-products.csv",
            serviceCsv = "https://primjer.hr/rijeka-services.csv",
        ),
        Outlet(
            name = "Opatija",
            code = "opatija",
            productCsv = "https://primjer.hr/opatija-products.csv",
            serviceCsv = "https://primjer.hr/opatija-services.csv",
        ),
        Outlet(
            name = "Čakovec Sjever",
            code = "cakovec",
            productCsv = "https://primjer.hr/cakovec-products.csv",
            serviceCsv = "https://primjer.hr/cakovec-services.csv",
        ),
    )

    @Test fun webshopIsPresentedSeparatelyFromActualBusinessCount() {
        val options = publishedLocations(outlets)
        assertEquals(4, options.size)
        assertEquals("", options.first().code)
        assertTrue(options.first().isWebshop)
        assertEquals("Webshop", options.first().name)
        assertEquals(3, options.count { !it.isWebshop })
        assertEquals("Korzo 12, Rijeka", options[1].address)
    }

    @Test fun outletSelectionUsesRealPublishedCodes() {
        val items = publishedLocations(outlets)
        assertEquals(
            listOf("rijeka-centar", "opatija", "cakovec"),
            items.drop(1).map { it.code },
        )
        assertFalse(items.drop(1).any { it.isWebshop })
    }

    @Test fun searchesNameAndCodeWithoutChangingTheOrder() {
        val items = publishedLocations(outlets)
        assertEquals(
            listOf("rijeka-centar"),
            filterPublishedLocations(items, "CENTAR rijeka").map { it.code },
        )
        assertEquals(
            listOf("cakovec"),
            filterPublishedLocations(items, "ČAKOVEC").map { it.code },
        )
        assertEquals(
            listOf("opatija"),
            filterPublishedLocations(items, "OPATIJA").map { it.code },
        )
    }

    @Test fun searchesPublishedAddressWithoutFabricatingLocationFields() {
        assertEquals(
            listOf("rijeka-centar"),
            filterPublishedLocations(publishedLocations(outlets), "Korzo 12").map { it.code },
        )
        assertTrue(publishedLocations(outlets)[2].address.isEmpty())
    }

    @Test fun webshopIsSearchableButNeverInventsAnotherPhysicalOutlet() {
        val items = publishedLocations(outlets)
        assertEquals(
            listOf(""),
            filterPublishedLocations(items, "online trgovina").map { it.code },
        )
        assertEquals(
            listOf(""),
            filterPublishedLocations(items, "WEBSHOP").map { it.code },
        )
        assertTrue(filterPublishedLocations(items, "Zagreb nepoznato").isEmpty())
    }

    @Test fun emptyOrWhitespaceQueryKeepsTheRealList() {
        val items = publishedLocations(outlets)
        assertEquals(items, filterPublishedLocations(items, ""))
        assertEquals(items, filterPublishedLocations(items, "  "))
        assertEquals(listOf(LocationOption("", "Webshop", true)), publishedLocations(emptyList()))
    }

    @Test fun longInputIsBoundedAndDoesNotCrash() {
        val items = publishedLocations(outlets)
        val query = "R".repeat(20000)
        assertTrue(filterPublishedLocations(items, query).isEmpty())
    }
}
