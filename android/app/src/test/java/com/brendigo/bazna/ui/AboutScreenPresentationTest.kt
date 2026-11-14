package com.brendigo.bazna.ui

import com.brendigo.bazna.data.CatalogIndex
import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.LegalContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutScreenPresentationTest {
    private fun snapshot(
        version: String? = "1.5.3",
        date: String? = "2026-10-10",
        page: String? = "https://primjer.hr/cjenici",
        legal: LegalContract? = null,
    ) = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = date,
            publicPage = page,
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = emptyList(),
            pluginVersion = version,
            legal = legal,
        ),
        products = emptyList(),
        services = emptyList(),
        archive = emptyList(),
        warnings = emptyList(),
    )

    @Test
    fun appVersionAndPluginVersionStayDistinct() {
        val info = aboutMetadata(snapshot(), "0.3.3")
        assertEquals("0.3.3", info.androidVersion)
        assertEquals("1.5.3", info.pluginVersion)
        assertEquals("WordPress", info.platformName)
        assertFalse(info.androidVersion == info.pluginVersion)
    }

    @Test
    fun aboutUsesActualBAZNAPlatformInsteadOfHardcodedWordPress() {
        val names = mapOf(
            "wordpress" to "WordPress",
            "web" to "Web PHP",
            "joomla" to "Joomla",
            "shopify" to "Shopify",
        )
        for ((platform, expected) in names) {
            val data = snapshot()
            val connected = data.copy(index = data.index.copy(platform = platform))
            assertEquals(expected, aboutMetadata(connected, "1.5.15").platformName)
        }
        assertEquals("BAZNA", baznaPlatformName("unknown"))
    }

    @Test
    fun unreportedWordPressVersionIsNotInvented() {
        val info = aboutMetadata(snapshot(version = null), "0.3.3")
        assertEquals("Nije objavljena", info.pluginVersion)
        assertEquals("Nije dostupna", aboutMetadata(snapshot(), "").androidVersion)
    }

    @Test
    fun missingPublicationDateIsExplicitlyMarked() {
        val info = aboutMetadata(snapshot(date = " "), "0.3.3")
        assertEquals("Datum nije objavljen", info.publishedAt)
        assertEquals("https://primjer.hr", info.site)
    }

    @Test
    fun publicPriceListLinkRequiresSameHttpsOrigin() {
        assertEquals(
            "https://primjer.hr/cjenici",
            aboutPublicPriceListUrl(snapshot()),
        )
        assertNull(aboutPublicPriceListUrl(snapshot(page = "http://primjer.hr/cjenici")))
        assertNull(aboutPublicPriceListUrl(snapshot(page = "https://evil.example/cjenici")))
        assertNull(aboutPublicPriceListUrl(snapshot(page = "https://primjer.hr.evil.example/cjenici")))
        assertNull(aboutPublicPriceListUrl(snapshot(page = "")))
        assertNull(aboutPublicPriceListUrl(snapshot(page = null)))
    }

    @Test
    fun invalidConnectedSiteCannotCreateOpenAction() {
        assertNull(
            aboutPublicPriceListUrl(snapshot().copy(site = "http://primjer.hr")),
        )
    }

    @Test
    fun legalProfileAppearsOnlyWhenActuallyPublished() {
        val absent = aboutMetadata(snapshot(), "0.3.3")
        assertNull(absent.rulesRevision)
        assertNull(absent.rulesEffectiveFrom)

        val legal = LegalContract(
            rulesRevision = "NN-101-2026-NN-110-2026",
            effectiveFrom = "2026-11-17",
            legacyReferenceDate = "2025-05-02",
            generalReferenceDate = "2026-09-10",
            archiveMinDays = 365,
            dailyDeadline = "08:00",
        )
        val published = aboutMetadata(snapshot(legal = legal), "0.3.3")
        assertEquals("NN-101-2026-NN-110-2026", published.rulesRevision)
        assertEquals("2026-11-17", published.rulesEffectiveFrom)
        assertTrue(published.rulesRevision!!.startsWith("NN-"))
    }
}
