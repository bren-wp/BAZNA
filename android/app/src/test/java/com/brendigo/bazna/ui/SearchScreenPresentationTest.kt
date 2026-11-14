package com.brendigo.bazna.ui

import com.brendigo.bazna.data.PriceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenPresentationTest {
    private val products = listOf(
        item("P-1", "Maslinovo ulje", "proizvod", "5,99", "8,49", "MU-01"),
        item("P-2", "Čokolada lješnjak", "proizvod", "1,49", "", "CO-02"),
    )
    private val services = listOf(
        item("S-1", "Servis klima-uređaja", "usluga", "45,00", "60,00", "SK-01"),
        item("S-2", "Čišćenje prostora", "usluga", "", "", "CP-02"),
    )

    @Test fun combinedSearchShowsBothRealCatalogFeedsWithoutDuplicates() {
        val results = searchCatalogEntries(products, services, "", 0)
        assertEquals(4, results.size)
        assertEquals(listOf("P-1", "P-2", "S-1", "S-2"), results.map { it.item.id })
        assertEquals(listOf(false, false, true, true), results.map { it.isService })
    }

    @Test fun typeFilterUsesActualFeedNotUntrustedKindLabel() {
        val productsOnly = searchCatalogEntries(products, services, "", 1)
        assertEquals(listOf("P-1", "P-2"), productsOnly.map { it.item.id })
        assertTrue(productsOnly.none { it.isService })
        val servicesOnly = searchCatalogEntries(products, services, "", 2)
        assertEquals(listOf("S-1", "S-2"), servicesOnly.map { it.item.id })
        assertTrue(servicesOnly.all { it.isService })
    }

    @Test fun searchMatchesCroatianLettersNamesAndSku() {
        assertEquals("P-2", searchCatalogEntries(products, services, "ČOKOLADA", 0).single().item.id)
        assertEquals("S-2", searchCatalogEntries(products, services, "čišćenje", 0).single().item.id)
        assertEquals("S-1", searchCatalogEntries(products, services, "sk-01", 0).single().item.id)
    }

    @Test fun multiTermSearchWorksAcrossBothLists() {
        val matching = searchCatalogEntries(products, services, "servis KLIMA", 0)
        assertEquals(listOf("S-1"), matching.map { it.item.id })
        assertTrue(searchCatalogEntries(products, services, "klima", 1).isEmpty())
    }

    @Test fun blankQueryAndEmptyCatalogStaySafe() {
        assertTrue(searchCatalogEntries(emptyList(), emptyList(), "", 0).isEmpty())
        assertEquals(4, searchCatalogEntries(products, services, "  ", 0).size)
        assertTrue(searchCatalogEntries(products, services, "nepostojeće", 0).isEmpty())
    }

    @Test fun pricesArePublishedValuesNeverPromotionalAssumptions() {
        assertTrue(searchResultPrice(products[0]).startsWith("5,99"))
        assertTrue(searchReferencePrice(products[0])?.startsWith("Bazna: 8,49") == true)
        assertNull(searchReferencePrice(products[1]))
        assertEquals("Cijena nije objavljena", searchResultPrice(services[1]))
        assertFalse(searchResultPrice(products[0]).contains("od ", ignoreCase = true))
    }

    private fun item(
        id: String,
        name: String,
        kind: String,
        current: String,
        reference: String,
        sku: String,
    ) = PriceItem(
        id = id,
        name = name,
        kind = kind,
        currentPrice = current,
        referencePrice = reference,
        referenceDate = "",
        sku = sku,
        availability = "",
        unit = "",
    )
}
