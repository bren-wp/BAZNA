package com.brendigo.bazna.ui

import com.brendigo.bazna.data.CatalogIndex
import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.Outlet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression: a WordPress feed URL alone is NOT proof of publication. */
class HomePublicationParityTest {
    private fun site(
        productReady: Boolean? = null,
        serviceReady: Boolean? = null,
        outletProductReady: Boolean? = null,
        outletServiceReady: Boolean? = null,
        allOutletsReady: Boolean? = null,
    ) = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "2026-11-14",
            publicPage = "https://primjer.hr/cjenici",
            archivePage = null,
            productsCsv = "https://primjer.hr/wp-json/brendigo-bazna/v1/cjenici/proizvodi/csv",
            servicesCsv = "https://primjer.hr/wp-json/brendigo-bazna/v1/cjenici/usluge/csv",
            outlets = listOf(Outlet(
                name = "Rijeka",
                code = "ri-01",
                productCsv = "https://primjer.hr/wp-json/brendigo-bazna/v1/cjenici/proizvodi/csv?oznaka=ri-01",
                serviceCsv = "https://primjer.hr/wp-json/brendigo-bazna/v1/cjenici/usluge/csv?oznaka=ri-01",
                productsCsvReady = outletProductReady,
                servicesCsvReady = outletServiceReady,
            )),
            productsCsvReady = productReady,
            servicesCsvReady = serviceReady,
            allOutletsCsvReady = allOutletsReady,
        ),
        products = emptyList(),
        services = emptyList(),
        archive = emptyList(),
        warnings = emptyList(),
    )

    @Test fun hiddenUnpublishedDocumentsDoNotInflateDashboard() {
        val none = site(productReady = false, serviceReady = false)
        assertEquals("0", homeMetrics(none)[2].count)
        assertTrue(homeCatalogPending(none))
        val one = site(productReady = true, serviceReady = false)
        assertEquals("1", homeMetrics(one)[2].count)
        val both = site(productReady = true, serviceReady = true)
        assertEquals("2", homeMetrics(both)[2].count)
        assertFalse(homeCatalogPending(both))
    }

    @Test fun outletReadinessIsIndependentOfWebshopAndAggregatePublication() {
        val source = site(
            productReady = true, serviceReady = true,
            outletProductReady = true, outletServiceReady = false,
            allOutletsReady = false,
        )
        assertEquals("2", homeMetrics(source)[2].count)
        assertEquals("1", homeMetrics(source, "ri-01")[2].count)
        assertTrue(homeCatalogPending(source))
        assertTrue(homeCatalogPending(source, "ri-01"))
        assertEquals("0", homeMetrics(source, "unknown")[2].count)
        assertTrue(homeCatalogPending(source, "unknown"))
    }

    @Test fun legacyApiWithoutReadinessMetadataStillKeepsWorking() {
        val old = site()
        assertEquals("2", homeMetrics(old)[2].count)
        assertFalse(homeCatalogPending(old))
        assertEquals("2", homeMetrics(old, "ri-01")[2].count)
    }
}
