package com.brendigo.bazna.ui

import com.brendigo.bazna.data.ArchiveEntry
import com.brendigo.bazna.data.CatalogIndex
import com.brendigo.bazna.data.CatalogSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveScreenPresentationTest {
    private fun entry(
        date: String,
        kind: String = "proizvodi",
        format: String = "csv",
        url: String = "https://primjer.hr/file.csv",
        outlet: String = "webshop",
    ): ArchiveEntry = ArchiveEntry(date, kind, outlet, format, url)

    private fun sample(items: List<ArchiveEntry>): CatalogSnapshot = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = null,
            publicPage = null,
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = emptyList(),
        ),
        products = emptyList(),
        services = emptyList(),
        archive = items,
        warnings = emptyList(),
    )

    @Test fun onlyActualCSVXMLFilesWithSameOriginHttpsAreShown() {
        val source = sample(listOf(
            entry("2026-10-01", url = "https://primjer.hr/ok.csv"),
            entry("2026-09-01", format = "XML", url = "https://primjer.hr/ok.xml"),
            entry("2026-08-01", format = "pdf", url = "https://primjer.hr/fake.pdf"),
            entry("2026-08-02", url = "http://primjer.hr/insecure.csv"),
            entry("2026-08-03", url = "https://other.example/outside.csv"),
            entry("2026-08-04", url = "https://primjer.hr.evil.example/evil.csv"),
        ))
        val published = publishedArchiveEntries(source)
        assertEquals(2, published.size)
        assertEquals(listOf("csv", "xml"), published.map { it.format })
        assertTrue(published.all { it.url.startsWith("https://primjer.hr/") })
    }

    @Test fun recentFilesSortFirstWhileMissingDatesSortLast() {
        val published = publishedArchiveEntries(sample(listOf(
            entry("", url = "https://primjer.hr/unknown.csv"),
            entry("2024-11-01", url = "https://primjer.hr/2024.csv"),
            entry("10. 10. 2026.", url = "https://primjer.hr/2026.csv"),
            entry("2025-06-01", url = "https://primjer.hr/2025.csv"),
        )))
        assertEquals(
            listOf("2026.csv", "2025.csv", "2024.csv", "unknown.csv"),
            published.map { it.url.substringAfterLast('/') },
        )
    }

    @Test fun yearChipsComeOnlyFromPublishedDates() {
        val items = publishedArchiveEntries(sample(listOf(
            entry("2026-10-01", url = "https://primjer.hr/a.csv"),
            entry("2025-12-31", url = "https://primjer.hr/b.csv"),
            entry("2026-09-01", url = "https://primjer.hr/c.csv"),
            entry("datum nije objavljen", url = "https://primjer.hr/d.csv"),
        )))
        assertEquals(listOf(2026, 2025), archiveAvailableYears(items))
        assertNull(archiveEntryYear(items.last()))
    }

    @Test fun combinedYearFormatAndSearchFiltersWork() {
        val items = publishedArchiveEntries(sample(listOf(
            entry("2026-10-09", "proizvodi", "csv", "https://primjer.hr/p.csv"),
            entry("2026-09-08", "usluge", "xml", "https://primjer.hr/s.xml"),
            entry("2025-10-09", "proizvodi", "xml", "https://primjer.hr/old.xml"),
        )))
        val result = filterPublishedArchive(items, "PROIZVODA listopad", 2026, "csv")
        assertEquals(listOf("https://primjer.hr/p.csv"), result.map { it.url })
        assertTrue(filterPublishedArchive(items, "nepostojeće", 2026, "sve").isEmpty())
        assertEquals(2, filterPublishedArchive(items, "", 2026, "sve").size)
        assertEquals(2, filterPublishedArchive(items, "", 0, "xml").size)
    }

    @Test fun duplicateUrlsAreDisplayedOnlyOnce() {
        val duplicated = publishedArchiveEntries(sample(listOf(
            entry("2026-10-01", url = "https://primjer.hr/same.csv"),
            entry("2026-10-01", url = "https://primjer.hr/same.csv"),
        )))
        assertEquals(1, duplicated.size)
    }

    @Test fun unknownTypeOrDateDoesNotInventSpecifics() {
        assertEquals("Cjenik", archiveDocumentTitle(entry("nepoznato", kind = "drugo")))
        assertEquals("Cjenik usluga",
            archiveDocumentTitle(entry("", kind = "usluge", format = "xml")))
        val named = archiveDocumentTitle(entry("2026-10-10", kind = "proizvodi"))
        assertTrue(named.contains("2026"))
        assertFalse(named.contains("pdf", ignoreCase = true))
    }

    @Test fun emptyOrInvalidSourcesReturnEmptyResults() {
        assertTrue(publishedArchiveEntries(sample(emptyList())).isEmpty())
        assertTrue(publishedArchiveEntries(
            sample(listOf(entry("2026-10-10"))).copy(site = "http://primjer.hr")
        ).isEmpty())
        assertTrue(filterPublishedArchive(emptyList(), "", 0, "csv").isEmpty())
    }
}
