package com.brendigo.bazna.data

import com.brendigo.bazna.ui.approvedProductImageUrl
import com.brendigo.bazna.ui.filterProductCatalog
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract between the WordPress public v1 API and Android Compose screens. */
class PublicCatalogCompatibilityTest {
    private val site = SiteUrl.parse("https://primjer.hr")

    private fun discovery() = JSONObject(
        """
        {
          "api_version": 1,
          "plugin_version": "1.5.3",
          "publication": {"csv_ready": true, "generated_at": "2026-10-31",
            "pending_changes": true, "compliance_certified": false},
          "feeds": {
            "proizvodi": {
              "csv": "https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici/proizvodi/csv",
              "available_csv": true
            },
            "usluge": {
              "csv": "https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici/usluge/csv",
              "available_csv": false
            }
          },
          "outlets": [{
            "code": "rijeka",
            "name": "Rijeka",
            "feeds": {
              "proizvodi": {
                "csv": "https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici/proizvodi/csv?oznaka=rijeka",
                "available_csv": true
              },
              "usluge": {
                "csv": "https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici/usluge/csv?oznaka=rijeka",
                "available_csv": false
              }
            }
          }]
        }
        """.trimIndent()
    )

    @Test fun readsActualPublishedStatusSeparatelyForEachTypeAndOutlet() {
        val index = CatalogDiscovery.parse(discovery(), site)
        assertEquals(true, index.productsCsvReady)
        assertEquals(false, index.servicesCsvReady)
        assertEquals(true, index.outlets.single().productsCsvReady)
        assertEquals(false, index.outlets.single().servicesCsvReady)
        assertEquals("rijeka", index.outlets.single().code)
    }

    @Test fun olderPluginWithoutReadinessFlagsStillConnects() {
        val payload = discovery()
        payload.getJSONObject("feeds").getJSONObject("proizvodi").remove("available_csv")
        payload.getJSONObject("feeds").getJSONObject("usluge").remove("available_csv")
        val outletFeeds = payload.getJSONArray("outlets").getJSONObject(0).getJSONObject("feeds")
        outletFeeds.getJSONObject("proizvodi").remove("available_csv")
        outletFeeds.getJSONObject("usluge").remove("available_csv")
        payload.remove("publication")
        val index = CatalogDiscovery.parse(payload, site)
        assertNull(index.productsCsvReady)
        assertNull(index.servicesCsvReady)
        assertNull(index.outlets.single().productsCsvReady)
        assertNull(index.outlets.single().servicesCsvReady)
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedReadyFlagCannotSpoofPublicationState() {
        val payload = discovery()
        payload.getJSONObject("feeds").getJSONObject("proizvodi")
            .put("available_csv", "yes")
        CatalogDiscovery.parse(payload, site)
    }

    @Test fun wordpressProductCsvOptionalImageUrlPreservesAllPriceFields() {
        val csv = """
            id;vrsta;naziv;sku;slika_url;jedinica_mjere;maloprodajna_cijena;sidrena_cijena;sidrena_datum;dostupnost
            7;proizvod;Maslinovo ulje;UL-1;https://primjer.hr/wp-content/uploads/ulje.jpg;1 L;5,99;8,49;2026-10-10;dostupno
            8;proizvod;Šampon;SH-1;;750 ml;2,89;3,99;2026-10-10;nedostupno
        """.trimIndent()
        val items = CsvParser.items(csv)
        assertEquals(2, items.size)
        assertEquals("https://primjer.hr/wp-content/uploads/ulje.jpg", items[0].imageUrl)
        assertEquals("5,99", items[0].currentPrice)
        assertEquals("8,49", items[0].referencePrice)
        assertEquals("2026-10-10", items[0].referenceDate)
        assertEquals("", items[1].imageUrl)
    }

    @Test fun productFiltersMatchActualPublicWordPressAvailability() {
        val csv = """
            id;vrsta;naziv;maloprodajna_cijena;sidrena_cijena;dostupnost
            1;proizvod;Maslinovo ulje;5,99;8,49;dostupno
            2;proizvod;Usluga dostave;4,00;;nedostupno
            3;proizvod;Čokolada;1,99;;dostupno
        """.trimIndent()
        val products = CsvParser.items(csv)
        assertEquals(3, filterProductCatalog(products, "", 0).size)
        assertEquals(listOf("1"), filterProductCatalog(products, "", 1).map { it.id })
        assertEquals(listOf("1", "3"), filterProductCatalog(products, "", 2).map { it.id })
        assertTrue(filterProductCatalog(products, "nepostojeći", 0).isEmpty())
    }

    @Test fun detectsPublishedButStaleWordPressCatalogWithoutCertifyingCompliance() {
        val index = CatalogDiscovery.parse(discovery(), site)
        assertEquals(true, index.pendingPublication)
        assertEquals(true, index.productsCsvReady)
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesFakeComplianceCertificationFromPublicApi() {
        val payload = discovery()
        payload.getJSONObject("publication").put("compliance_certified", true)
        CatalogDiscovery.parse(payload, site)
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesStringInsteadOfBooleanForPendingChanges() {
        val payload = discovery()
        payload.getJSONObject("publication").put("pending_changes", "yes")
        CatalogDiscovery.parse(payload, site)
    }

    @Test fun retainsUnitPriceSaleLabelAndReferenceDateAsSeparateFields() {
        val csv = """
            id;vrsta;naziv;maloprodajna_cijena;cijena_jedinice_mjere;naziv_posebnog_oblika;sidrena_cijena;sidrena_datum;barkod;marka
            11;proizvod;Sapun;2,45;4,90;akcija;3,20;02.05.2025.;3851234567891;Domaće
        """.trimIndent()
        val row = CsvParser.items(csv).single()
        assertEquals("2,45", row.currentPrice)
        assertEquals("4,90", row.unitPrice)
        assertEquals("akcija", row.saleLabel)
        assertEquals("3,20", row.referencePrice)
        assertEquals("02.05.2025.", row.referenceDate)
        assertEquals("3851234567891", row.barcode)
        assertEquals("Domaće", row.brand)
    }

    @Test fun imageIsOnlyApprovedOnTheSameHttpsWordPressOrigin() {
        val url = "https://primjer.hr/wp-content/uploads/thumb.webp"
        assertEquals(url, approvedProductImageUrl("https://primjer.hr", url))
        assertNull(approvedProductImageUrl("https://primjer.hr", "http://primjer.hr/thumb.jpg"))
        assertNull(approvedProductImageUrl("https://primjer.hr", "https://evil.example/thumb.jpg"))
        assertNull(approvedProductImageUrl("https://primjer.hr", "https://primjer.hr.evil.example/thumb.jpg"))
        assertNull(approvedProductImageUrl("https://primjer.hr", ""))
    }

    @Test fun imageColumnIsOptionalForOlderPublishedCsv() {
        val items = CsvParser.items(
            "id;vrsta;naziv;maloprodajna_cijena\n1;proizvod;Bez fotografije;2,50"
        )
        assertEquals("", items.single().imageUrl)
        assertFalse(items.single().name.isBlank())
    }
}
