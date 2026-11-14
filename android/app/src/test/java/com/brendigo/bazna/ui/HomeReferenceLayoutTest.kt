package com.brendigo.bazna.ui

import com.brendigo.bazna.data.ArchiveEntry
import com.brendigo.bazna.data.CatalogIndex
import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.Outlet
import com.brendigo.bazna.data.PriceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Homepage reference screen 03 uses real four-card metrics and two real docs.
 * No sample/demo totals are ever pulled from the branding screenshot.
 */
class HomeReferenceLayoutTest {
    private fun sample(): CatalogSnapshot = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "2026-10-31",
            publicPage = "https://primjer.hr/cjenici",
            archivePage = null,
            productsCsv = "https://primjer.hr/products.csv",
            servicesCsv = "https://primjer.hr/services.csv",
            outlets = listOf(
                Outlet("Rijeka", "rijeka",
                    "https://primjer.hr/rijeka-products.csv",
                    "https://primjer.hr/rijeka-services.csv"),
            ),
        ),
        products = listOf(
            PriceItem("1", "Proizvod", "proizvod", "3,50", "", "", "", "", ""),
        ),
        services = listOf(
            PriceItem("2", "Usluga", "usluga", "45,00", "", "", "", "", ""),
            PriceItem("3", "Usluga 2", "usluga", "70,00", "", "", "", "", ""),
        ),
        archive = listOf(
            ArchiveEntry("2026-10-30", "usluge", "", "xml",
                "https://primjer.hr/service-archive.xml"),
            ArchiveEntry("2026-10-28", "proizvodi", "", "csv",
                "https://primjer.hr/product-archive.csv"),
            ArchiveEntry("2026-10-22", "proizvodi", "", "csv",
                "https://primjer.hr/old.csv"),
        ),
        warnings = emptyList(),
    )

    @Test fun fourReferenceMetricCardsUseRealCounts() {
        val values = homeMetrics(sample())
        assertEquals(listOf("Proizvodi", "Usluge", "Cjenici", "Arhiva"),
            values.map { it.label })
        assertEquals(listOf("1", "2", "2", "3"), values.map { it.count })
        assertEquals(listOf("products", "services", "cjenici", "archive"),
            values.map { it.route })
        assertFalse(values.joinToString().contains("1.248"))
    }

    @Test fun activeOutletCountsOnlyItsOwnRealCsvFeeds() {
        val source = sample()
        assertEquals("2", homeMetrics(source, "rijeka")[2].count)
        assertEquals("0", homeMetrics(source, "unknown")[2].count)
        assertEquals("2", homeMetrics(source, "")[2].count)
    }

    @Test fun missingOrCrossOriginFeedsAreNeverCounted() {
        val source = sample()
        val dirty = source.copy(index = source.index.copy(
            productsCsv = "http://primjer.hr/nonhttps.csv",
            servicesCsv = "https://other.example/foreign.csv",
        ))
        assertEquals("0", homeMetrics(dirty)[2].count)
    }

    @Test fun recentListMatchesThreeRealPublicationsInReference() {
        val actual = homeRecentPublications(sample())
        assertEquals(3, actual.size)
        assertEquals(listOf("CSV", "CSV", "XML"), actual.map { it.format })
        assertTrue(actual.all { it.url.startsWith("https://primjer.hr/") })
        assertTrue(actual.all { it.date?.contains("2026") == true })
    }

    @Test fun archiveIsUsedWhenThereAreNoCurrentFeeds() {
        val initial = sample()
        val source = initial.copy(index = initial.index.copy(
            productsCsv = "", servicesCsv = "",
        ))
        val items = homeRecentPublications(source)
        assertEquals(3, items.size)
        assertEquals("XML", items.first().format)
        assertEquals("Cjenik usluga", items.first().title)
        assertEquals("Cjenik proizvoda", items[1].title)
    }

    @Test fun invalidPublicUrlsCannotBecomeClickableRecentRows() {
        val initial = sample()
        val source = initial.copy(
            index = initial.index.copy(
                productsCsv = "https://evil.example/p.csv",
                servicesCsv = "http://primjer.hr/s.csv",
            ),
            archive = listOf(
                ArchiveEntry("2026-10-31", "usluge", "", "xml",
                    "https://evil.example/a.xml"),
            ),
        )
        assertTrue(homeRecentPublications(source).isEmpty())
    }

    @Test fun connectedSiteLabelShowsHostNotAnInventedStoreName() {
        assertEquals("primjer.hr", homeSiteLabel("https://primjer.hr"))
        assertEquals("primjer.hr", homeSiteLabel("https://primjer.hr/path"))
        assertEquals("trgovina.example.hr", homeSiteLabel("https://trgovina.example.hr/"))
    }

    @Test fun publishedCsvCanStillBeStaleAfterNewWordPressPriceEdits() {
        val data = sample().copy(
            index = sample().index.copy(
                productsCsvReady = true,
                servicesCsvReady = true,
                pendingPublication = true,
            ),
        )
        assertTrue(homeCatalogPending(data))
        assertFalse(homeCatalogPending(data.copy(
            index = data.index.copy(pendingPublication = false),
        )))
    }

    @Test fun aPendingWordPressFeedIsNotShownAsFullyPublished() {
        val source = sample()
        val pending = source.copy(
            index = source.index.copy(productsCsvReady = true, servicesCsvReady = false),
        )
        assertTrue(homeCatalogPending(pending))
        assertFalse(homeCatalogPending(pending.copy(
            index = pending.index.copy(servicesCsvReady = true),
        )))
    }

    @Test fun legacyPluginWithNoReadinessRemainsCompatible() {
        assertFalse(homeCatalogPending(sample()))
    }

    @Test fun outletReadinessCannotBorrowWebshopStatus() {
        val source = sample()
        val pending = source.copy(
            index = source.index.copy(
                productsCsvReady = true,
                servicesCsvReady = true,
                outlets = listOf(Outlet(
                    "Rijeka", "rijeka", "https://primjer.hr/ri-p.csv",
                    "https://primjer.hr/ri-s.csv",
                    productsCsvReady = false,
                    servicesCsvReady = true,
                )),
            ),
        )
        assertFalse(homeCatalogPending(pending))
        assertTrue(homeCatalogPending(pending, "rijeka"))
        assertTrue(homeCatalogPending(pending, "deleted-outlet"))
    }

    @Test fun dateSortingUsesActualPublishedIsoAndCroatianDates() {
        val date = homeDateKey("31. 10. 2026.")
        assertEquals(date, homeDateKey("2026-10-31"))
        assertTrue(date > homeDateKey("2026-10-30"))
    }
}
