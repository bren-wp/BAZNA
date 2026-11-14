package com.brendigo.bazna.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogDiscoveryTest {
    private val site = SiteUrl.parse("https://primjer.hr")
    private fun payload(): JSONObject = JSONObject(
        """
        {
          "api_version":1,
          "plugin_version":"1.5.2",
          "feeds":{
            "proizvodi":{"csv":"https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici/proizvodi/csv"},
            "usluge":{"csv":"https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici/usluge/csv"}
          },
          "outlets":[
            {"name":"Rijeka","code":"RI-01","feeds":{
              "proizvodi":{"csv":"https://primjer.hr/proizvodi.csv?oznaka=RI-01"},
              "usluge":{"csv":"https://primjer.hr/usluge.csv?oznaka=RI-01"}
            }}
          ]
        }
        """.trimIndent()
    )

    @Test fun validWordPressDiscoveryUsesPublicSameOrigin() {
        val index = CatalogDiscovery.parse(payload(), site)
        assertEquals("RI-01", index.outlets.single().code)
        assertEquals("1.5.2", index.pluginVersion)
        assertEquals("https://primjer.hr/usluge.csv?oznaka=RI-01", index.outlets.single().serviceCsv)
    }

    @Test fun publishedInquiryEndpointUsesOnlySameOriginHttps() {
        val url = "https://primjer.hr/wp-json/brendigo-bazna/v1/usluge-na-upit"
        assertEquals(url, CatalogDiscovery.parse(payload().put("on_request", url), site).onRequestUrl)
        assertEquals(null, CatalogDiscovery.parse(payload(), site).onRequestUrl)
        for (bad in listOf("http://primjer.hr/upit", "https://evil.example/upit",
            "https://primjer.hr.evil.test/upit")) {
            assertThrows(IllegalArgumentException::class.java) {
                CatalogDiscovery.parse(payload().put("on_request", bad), site)
            }
        }
    }

    @Test fun optionalPublishedOutletAddressesAreBackwardCompatible() {
        val json = payload()
        val outlet = json.getJSONArray("outlets").getJSONObject(0)
        assertEquals("", CatalogDiscovery.parse(json, site).outlets.single().address)
        outlet.put("address", "Korzo 12, Rijeka")
        assertEquals(
            "Korzo 12, Rijeka",
            CatalogDiscovery.parse(json, site).outlets.single().address,
        )
        for (invalid in listOf(42, "A".repeat(401), "Ulica\nprivatno")) {
            outlet.put("address", invalid)
            assertThrows(IllegalArgumentException::class.java) {
                CatalogDiscovery.parse(json, site)
            }
        }
    }

    @Test fun everyPlatformWorksWithoutAnyActivationField() {
        for (platform in listOf("wordpress", "web", "joomla", "shopify")) {
            val result = CatalogDiscovery.parse(payload().put("platform", platform), site)
            assertEquals(platform, result.platform)
            assertEquals("RI-01", result.outlets.single().code)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.parse(payload().put("platform", "unknown"), site)
        }
    }

    @Test fun supportsLegacyPluginWithoutOptionalLegalProfile() {
        val index = CatalogDiscovery.parse(payload().apply { remove("plugin_version") }, site)
        assertEquals(null, index.legal)
    }

    @Test fun rejectsUnexpectedAuthenticationAndUnsafeConnectionModes() {
        val valid = JSONObject(
            """{"mode":"public_read_only","authentication_required":false,
            "csv_publication_required":true,
            "rest_endpoint":"https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici"}"""
        )
        assertEquals("1.5.2", CatalogDiscovery.parse(payload().put("connection", valid), site).pluginVersion)
        for (mutated in listOf(
            JSONObject(valid.toString()).put("mode", "admin_write"),
            JSONObject(valid.toString()).put("authentication_required", true),
            JSONObject(valid.toString()).put("csv_publication_required", false),
            JSONObject(valid.toString()).put("rest_endpoint", "https://other.example/api"),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                CatalogDiscovery.parse(payload().put("connection", mutated), site)
            }
        }
    }

    @Test fun legacyLicenseMetadataIsIgnoredRatherThanBlockingCatalog() {
        val old = payload().put("licensing", JSONObject().put("required", true).put("active", false))
        val result = CatalogDiscovery.parse(old, site)
        assertEquals("https://primjer.hr/usluge.csv?oznaka=RI-01",
            result.outlets.single().serviceCsv)
        // License status is obsolete; ignore malformed legacy metadata too.
        old.put("licensing", JSONObject().put("required", "wrong").put("active", "no"))
        assertEquals("wordpress", CatalogDiscovery.parse(old, site).platform)
    }

    @Test fun rejectsUnknownOrMissingApiVersion() {
        for (value in listOf(0, 2, 99)) {
            assertThrows(IllegalArgumentException::class.java) {
                CatalogDiscovery.parse(payload().put("api_version", value), site)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.parse(payload().apply { remove("api_version") }, site)
        }
    }

    @Test fun rejectsDuplicateOutletsCaseInsensitively() {
        val json = payload()
        val outlets = json.getJSONArray("outlets")
        val duplicated = JSONObject(outlets.getJSONObject(0).toString()).put("code", "ri-01")
        outlets.put(duplicated)
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.parse(json, site)
        }
    }

    @Test fun rejectsOversizedOutletsInsteadOfTruncating() {
        val json = payload().put("outlets", org.json.JSONArray().also { array ->
            repeat(501) { array.put(JSONObject()) }
        })
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.parse(json, site)
        }
    }

    @Test fun rejectsExternalLinksAndIncompleteFeeds() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.parse(payload().apply {
                getJSONObject("feeds").getJSONObject("usluge")
                    .put("csv", "https://nedozvoljeno.hr/feed.csv")
            }, site)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.parse(payload().apply {
                getJSONObject("feeds").remove("proizvodi")
            }, site)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.parse(payload().put("legal", "unknown"), site)
        }
    }

    @Test fun rejectsUnknownOutletCodeSyntax() {
        val json = payload()
        json.getJSONArray("outlets").getJSONObject(0).put("code", "../other")
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDiscovery.parse(json, site)
        }
    }
}
