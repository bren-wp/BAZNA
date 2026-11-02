package com.brendigo.sidrena.ui

import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.Outlet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsScreenPresentationTest {

    private fun catalog(
        published: String? = "10. 10. 2026.",
        plugin: String? = "1.5.3",
    ): CatalogSnapshot = CatalogSnapshot(
        site = "https://trgovina.example.hr",
        index = CatalogIndex(
            publishedAt = published,
            publicPage = null,
            archivePage = null,
            productsCsv = "",
            servicesCsv = "",
            outlets = listOf(
                Outlet("Rijeka", "rijeka", "", ""),
                Outlet("Opatija", "opatija", "", ""),
            ),
            pluginVersion = plugin,
        ),
        products = emptyList(),
        services = emptyList(),
        archive = emptyList(),
        warnings = emptyList(),
    )

    @Test fun showsRealLinkedWebsiteAndPublicationDate() {
        val summary = settingsSummary(catalog(), "rijeka")
        assertEquals("https://trgovina.example.hr", summary.site)
        assertEquals("10. 10. 2026.", summary.publishedAt)
        assertEquals("1.5.3", summary.pluginVersion)
    }

    @Test fun outletNameMustBeResolvedFromActualPublishedCode() {
        assertEquals("Rijeka", settingsSummary(catalog(), "rijeka").outletName)
        assertEquals("Opatija", settingsSummary(catalog(), "opatija").outletName)
        assertEquals("Webshop", settingsSummary(catalog(), "").outletName)
    }

    @Test fun unknownOutletIsNotSilentlyPresentedAsWebshop() {
        val summary = settingsSummary(catalog(), "unknown-outlet")
        assertEquals("Poslovnica nije dostupna", summary.outletName)
        assertFalse(summary.outletName == "Webshop")
    }

    @Test fun unpublishedMetadataIsMarkedUnavailableNotInvented() {
        val summary = settingsSummary(catalog(published = null, plugin = null), "")
        assertEquals("Datum nije objavljen", summary.publishedAt)
        assertEquals("Nije objavljena", summary.pluginVersion)
        assertEquals("Nije dostupan", summary.rulesRevision)
    }

    @Test fun blankFieldsDoNotAppearAsFalseStatus() {
        val summary = settingsSummary(catalog(published = " ", plugin = ""), "")
        assertEquals("Datum nije objavljen", summary.publishedAt)
        assertEquals("Nije objavljena", summary.pluginVersion)
    }

    @Test fun anEmptyOutletNameDoesNotBecomeAnInventedBusiness() {
        val source = catalog()
        val emptyNameOutlet = source.copy(
            index = source.index.copy(
                outlets = listOf(Outlet("", "code-without-name", "", "")),
            ),
        )
        assertEquals(
            "Poslovnica nije dostupna",
            settingsSummary(emptyNameOutlet, "code-without-name").outletName,
        )
    }
}
