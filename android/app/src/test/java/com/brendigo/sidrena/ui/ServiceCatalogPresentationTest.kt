package com.brendigo.sidrena.ui

import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.Outlet
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


    @Test fun anchoredServiceReferenceAlwaysKeepsItsActualPublishedDate() {
        assertEquals(
            "Ref. cijena: 60,00 € · 2026-10-09",
            serviceReferenceWithDate("60,00", "2026-10-09"),
        )
        assertEquals("Ref. cijena: 60,00 €", serviceReferenceWithDate("60,00", ""))
        assertNull(serviceReferenceWithDate("", "2026-10-09"))
    }

    @Test fun servicePublicationStatusRespectsOutletWithoutWebshopFallback() {
        val site = CatalogSnapshot(
            site = "https://primjer.hr",
            index = CatalogIndex(
                publishedAt = null,
                publicPage = null,
                archivePage = null,
                productsCsv = "https://primjer.hr/p.csv",
                servicesCsv = "https://primjer.hr/s.csv",
                outlets = listOf(
                    Outlet(
                        name = "Rijeka",
                        code = "ri",
                        productCsv = "https://primjer.hr/p-ri.csv",
                        serviceCsv = "https://primjer.hr/s-ri.csv",
                        servicesCsvReady = false,
                    ),
                ),
                servicesCsvReady = true,
            ),
            products = emptyList(),
            services = emptyList(),
            archive = emptyList(),
            warnings = emptyList(),
        )
        assertEquals(true, servicePublicationState(site))
        assertEquals(false, servicePublicationState(site, "ri"))
        assertEquals(false, servicePublicationState(site, "unknown"))
        assertNull(servicePublicationState(site.copy(
            index = site.index.copy(servicesCsvReady = null),
        )))
    }

    @Test fun archivedServicePricesNeverBecomeInventedStartingPrices() {
        assertFalse(serviceCurrentPrice("100.00").contains("od ", ignoreCase = true))
        assertEquals("Cijena nije objavljena", serviceCurrentPrice(" "))
        assertEquals(listOf("2"), visiblePublicServices(services, "Montaža", 2).map { it.id })
    }

    private fun service(id: String, name: String, price: String, reference: String) =
        PriceItem(id, name, "usluga", price, reference, "2026-10-01",
            "SKU-" + id, "", "")
}
