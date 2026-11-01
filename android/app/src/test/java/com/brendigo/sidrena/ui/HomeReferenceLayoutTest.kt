package com.brendigo.sidrena.ui

import com.brendigo.sidrena.data.ArchiveEntry
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.Outlet
import com.brendigo.sidrena.data.PriceItem
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

    @Test fun recentListIsLimitedToTwoRealPublications() {
        val actual = homeRecentPublications(sample())
        assertEquals(2, actual.size)
        assertEquals(listOf("CSV", "CSV"), actual.map { it.format })
        assertTrue(actual.all { it.url.startsWith("https://primjer.hr/") })
        assertTrue(actual.all { it.date?.contains("2026") == true })
    }

    @Test fun archiveIsUsedWhenThereAreNoCurrentFeeds() {
        val initial = sample()
        val source = initial.copy(index = initial.index.copy(
            productsCsv = "", servicesCsv = "",
        ))
        val items = homeRecentPublications(source)
        assertEquals(2, items.size)
        assertEquals("XML", items.first().format)
        assertEquals("Cjenik usluga", items.first().title)
        assertEquals("Cjenik proizvoda", items.last().title)
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

    @Test fun dateSortingUsesActualPublishedIsoAndCroatianDates() {
        val date = homeDateKey("31. 10. 2026.")
        assertEquals(date, homeDateKey("2026-10-31"))
        assertTrue(date > homeDateKey("2026-10-30"))
    }
}
