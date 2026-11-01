package com.brendigo.sidrena.ui

import com.brendigo.sidrena.data.ArchiveEntry
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.Outlet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicPriceListsScreenTest {

    private fun sample(): CatalogSnapshot = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "2026-10-11",
            publicPage = "https://primjer.hr/cjenici",
            archivePage = "https://primjer.hr/arhiva",
            productsCsv = "https://primjer.hr/proizvodi.csv",
            servicesCsv = "https://primjer.hr/usluge.csv",
            outlets = listOf(
                Outlet(
                    name = "Rijeka",
                    code = "rijeka",
                    productCsv = "https://primjer.hr/rijeka/proizvodi.csv",
                    serviceCsv = "https://primjer.hr/rijeka/usluge.csv",
                ),
            ),
        ),
        products = emptyList(),
        services = emptyList(),
        archive = listOf(
            ArchiveEntry("2026-10-09", "proizvodi", "", "xml", "https://primjer.hr/a.xml"),
            ArchiveEntry("2026-10-10", "usluge", "", "csv", "https://primjer.hr/b.csv"),
            ArchiveEntry("2026-10-10", "usluge", "", "csv", "https://primjer.hr/b.csv"),
            ArchiveEntry("2026-10-08", "drugo", "", "pdf", "https://primjer.hr/fake.pdf"),
        ),
        warnings = emptyList(),
    )

    @Test fun currentDocumentsAndArchiveAreRealAndDistinct() {
        val all = publicPriceListDocuments(sample())
        assertEquals(5, all.size)
        assertEquals(listOf("CSV", "CSV", "WEB", "CSV", "XML"), all.map { it.format })
        assertEquals(listOf(false, false, false, true, true), all.map { it.archived })
        assertEquals("https://primjer.hr/b.csv", all[3].url)
        assertFalse(all.any { it.format == "PDF" })
    }

    @Test fun selectedOutletUsesItsOwnPublishedFeeds() {
        val list = publicPriceListDocuments(sample(), selectedOutlet = "rijeka")
        assertEquals("https://primjer.hr/rijeka/proizvodi.csv", list[0].url)
        assertEquals("https://primjer.hr/rijeka/usluge.csv", list[1].url)
        assertTrue(publicPriceListDocuments(sample(), selectedOutlet = "missing").isEmpty())
    }

    @Test fun filtersUseRealKindAndCaseInsensitiveTerms() {
        val list = publicPriceListDocuments(sample())
        assertEquals(2, filterPublicPriceLists(list, "", 1).size)
        assertEquals(2, filterPublicPriceLists(list, "", 2).size)
        assertEquals(1, filterPublicPriceLists(list, "", 3).size)
        assertEquals(1, filterPublicPriceLists(list, "ARHIVIRANI proizvoda", 1).size)
        assertEquals(1, filterPublicPriceLists(list, "wEb", 3).size)
        assertTrue(filterPublicPriceLists(list, "pdf", 0).isEmpty())
    }

    @Test fun maliciousExternalOrHttpLinksCannotBeDisplayed() {
        val source = sample().copy(
            index = sample().index.copy(
                productsCsv = "http://primjer.hr/proizvodi.csv",
                servicesCsv = "https://other.example/usluge.csv",
                publicPage = "https://primjer.hr.evil.example/cjenici",
            ),
            archive = listOf(
                ArchiveEntry("2026-10-11", "proizvodi", "", "csv",
                    "https://evil.example/remote.csv"),
            ),
        )
        assertTrue(publicPriceListDocuments(source).isEmpty())
    }

    @Test fun emptyAndInvalidSitesDoNotInventDocuments() {
        val source = sample()
        assertEquals(0, publicPriceListDocuments(source.copy(site = "http://primjer.hr")).size)
        val noPublic = source.copy(
            index = source.index.copy(productsCsv = "", servicesCsv = "", publicPage = null),
            archive = emptyList(),
        )
        assertTrue(publicPriceListDocuments(noPublic).isEmpty())
    }

    @Test fun archiveKindClassificationIsGroundedInPublishedField() {
        assertEquals(1, priceListGroup("proizvodi"))
        assertEquals(2, priceListGroup("Usluge"))
        assertEquals(3, priceListGroup("nepoznato"))
        assertEquals(3, priceListGroup(""))
    }
}
