package com.brendigo.sidrena.ui

import com.brendigo.sidrena.data.ArchiveEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomePresentationTest {

    @Test
    fun isoAndCroatianDatesProduceSameDayKey() {
        val expected = LocalDate.of(2026, 10, 10).toEpochDay()
        assertEquals(expected, homeDateKey("2026-10-10T14:20:00"))
        assertEquals(expected, homeDateKey("10. 10. 2026. 14:20"))
    }

    @Test
    fun malformedDatesDoNotCrashTheHomeScreen() {
        assertEquals(Long.MIN_VALUE, homeDateKey("Datum nije dostupan"))
        assertEquals(Long.MIN_VALUE, homeDateKey("31. 02. 2026."))
    }

    @Test
    fun recentArchiveIsDistinctSortedAndLimitedToThree() {
        val entries = listOf(
            entry("2026-10-08", "a"),
            entry("09. 10. 2026.", "b"),
            entry("10. 10. 2026.", "c"),
            entry("2026-10-11", "d"),
            entry("2026-10-11", "d"),
        )
        val recent = homeRecentArchive(entries)
        assertEquals(listOf("d", "c", "b"), recent.map { it.url })
    }

    @Test
    fun archiveLabelsUseActualKindNotInventedActivity() {
        assertEquals("Cjenik proizvoda", homeArchiveLabel(entry("2026-10-01", "p", "proizvodi")))
        assertEquals("Cjenik usluga", homeArchiveLabel(entry("2026-10-01", "s", "usluge")))
        assertEquals("Arhivirani cjenik", homeArchiveLabel(entry("2026-10-01", "x", "drugo")))
    }

    @Test
    fun emptyArchiveProducesNoFalseUpdateRows() {
        assertTrue(homeRecentArchive(emptyList()).isEmpty())
    }

    private fun entry(date: String, url: String, kind: String = "proizvodi") =
        ArchiveEntry(date, kind, "webshop", "csv", url)
}
