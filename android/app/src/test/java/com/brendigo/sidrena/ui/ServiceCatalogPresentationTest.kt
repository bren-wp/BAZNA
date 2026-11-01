package com.brendigo.sidrena.ui

import com.brendigo.sidrena.data.PriceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceCatalogPresentationTest {
    private val services = listOf(
        service("1", "Servis klima-uređaja", "45,00", "60,00"),
        service("2", "Montaža rasvjete", "30,00", ""),
        service("3", "Čišćenje prostora", "20,00", "35,00"),
    )

    @Test fun searchMatchesCroatianLettersAndPublishedIdentifiers() {
        assertEquals("1", visiblePublicServices(services, "KLIMA", 0).single().id)
        assertEquals("3", visiblePublicServices(services, "čišĆENJE", 0).single().id)
        assertEquals("2", visiblePublicServices(services, "SKU-2", 0).single().id)
    }

    @Test fun filterOnlyUsesPublishedAnchors() {
        assertEquals(listOf("1", "3"), visiblePublicServices(services, "", 1).map { it.id })
        assertEquals(listOf("2"), visiblePublicServices(services, "", 2).map { it.id })
        assertTrue(visiblePublicServices(services, "Montaža", 1).isEmpty())
    }

    @Test fun servicePricesDoNotInventFromOrDiscountSemantics() {
        val price = serviceCurrentPrice("45,00")
        assertTrue(price.startsWith("45,00"))
        assertFalse(price.contains("od ", ignoreCase = true))
        assertEquals("Cijena nije objavljena", serviceCurrentPrice(""))
    }

    @Test fun missingReferencePriceIsNotFabricated() {
        assertNull(serviceReferenceCaption(""))
        assertNull(serviceReferenceCaption("  "))
        val caption = serviceReferenceCaption("60,00")
        assertTrue(caption != null && caption.startsWith("Ref. cijena: 60,00"))
    }

    @Test fun anEmptyServiceListIsSafe() {
        assertTrue(visiblePublicServices(emptyList(), "", 0).isEmpty())
        assertTrue(visiblePublicServices(emptyList(), "xx", 1).isEmpty())
    }

    private fun service(id: String, name: String, price: String, reference: String) =
        PriceItem(id, name, "usluga", price, reference, "2026-10-01",
            "SKU-" + id, "", "")
}
