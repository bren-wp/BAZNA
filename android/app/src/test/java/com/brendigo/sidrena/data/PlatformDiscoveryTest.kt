package com.brendigo.sidrena.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformDiscoveryTest {
    @Test fun wordpressKeepsOriginalRestAliases() {
        val result = platformDiscoveryCandidates(SiteUrl.parse("https://primjer.hr"))
        assertEquals("https://primjer.hr/wp-json/brendigo-bazna/v1/cjenici", result[0].toASCIIString())
        assertTrue(result[1].toASCIIString().contains("rest_route="))
        assertEquals("https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici", result[2].toASCIIString())
        assertTrue(result[3].toASCIIString().contains("rest_route="))
        assertEquals("https://primjer.hr/bazna/v1/cjenici", result[4].toASCIIString())
    }

    @Test fun standaloneAndJoomlaShareOwnPublicBAZNAEndpoint() {
        val result = platformDiscoveryCandidates(SiteUrl.parse("https://primjer.hr/podmapa"))
        assertEquals("https://primjer.hr/podmapa/bazna/v1/cjenici", result[4].toASCIIString())
        assertTrue(result.all { it.scheme == "https" && it.host == "primjer.hr" })
    }

    @Test fun shopifyProxyUsesSameStorefrontDomain() {
        val site = SiteUrl.parse("https://trgovina.myshopify.com")
        val result = platformDiscoveryCandidates(site)
        assertEquals(
            "https://trgovina.myshopify.com/apps/bazna/v1/cjenici",
            result[5].toASCIIString(),
        )
        assertTrue(result.all { it.host == site.base.host })
    }

    @Test fun customShopifyProxyBaseRequiresExplicitUserUrl() {
        val site = SiteUrl.parse("https://trgovina.myshopify.com/apps/posebno")
        assertEquals(
            "https://trgovina.myshopify.com/apps/posebno/v1/cjenici",
            platformDiscoveryCandidates(site).last().toASCIIString(),
        )
    }
}
