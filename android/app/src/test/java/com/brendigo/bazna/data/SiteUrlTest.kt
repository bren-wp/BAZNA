package com.brendigo.bazna.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.json.JSONException
import java.io.IOException
import org.junit.Assert.assertThrows
import org.junit.Test

class SiteUrlTest {
    @Test fun supportsWordPressSubdirectories() {
        val site = SiteUrl.parse("https://primjer.hr/trgovina")
        assertEquals("https://primjer.hr/trgovina/wp-json/brendigo-sidrena/v1/cjenici",
            site.endpoint("wp-json/brendigo-sidrena/v1/cjenici").toASCIIString())
    }

    @Test fun bareWordPressDomainConnectsOverHttpsAutomatically() {
        val bare = SiteUrl.parse("primjer.hr")
        assertEquals("https://primjer.hr", bare.display)
        val subdirectory = SiteUrl.parse("www.primjer.hr/trgovina")
        assertEquals("https://www.primjer.hr/trgovina", subdirectory.display)
        assertEquals(
            "https://www.primjer.hr/trgovina/wp-json/brendigo-sidrena/v1/cjenici",
            subdirectory.endpoint("wp-json/brendigo-sidrena/v1/cjenici").toASCIIString(),
        )
        // Every accepted shorthand is normalized to HTTPS, never HTTP.
        assertEquals("https://primjer.hr", SiteUrl.parse("  PRIMJER.HR  ").display)
    }

    @Test fun supportsSameOriginPlainPermalinkRestApiWithoutCredentials() {
        val root = SiteUrl.parse("https://primjer.hr")
        assertEquals(
            "https://primjer.hr/?rest_route=%2Fbrendigo-sidrena%2Fv1%2Fcjenici",
            root.queryRestEndpoint("brendigo-sidrena/v1/cjenici").toASCIIString(),
        )
        val subdir = SiteUrl.parse("https://primjer.hr/trgovina")
        assertEquals(
            "https://primjer.hr/trgovina/?rest_route=%2Fbrendigo-sidrena%2Fv1%2Fcjenici",
            subdir.queryRestEndpoint("brendigo-sidrena/v1/cjenici").toASCIIString(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            root.queryRestEndpoint("https://other.example/v1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            root.queryRestEndpoint("../wp-admin")
        }
    }

    @Test fun rejectsPlainHttpAndLocalHosts() {
        for (url in listOf("http://primjer.hr", "https://localhost", "https://127.0.0.1",
            "https://admin:lozinka@primjer.hr", "https://primjer.hr/a/../wp")) {
            assertThrows(IllegalArgumentException::class.java) { SiteUrl.parse(url) }
        }
    }

    @Test fun fallbackNeverBypassesAuthorizationOrNetworkSecurity() {
        assertTrue(shouldFallbackToPlainRest(IOException("HTTP 404")))
        assertTrue(shouldFallbackToPlainRest(IOException("HTTP 405")))
        assertTrue(shouldFallbackToPlainRest(JSONException("Server returned HTML")))
        assertFalse(shouldFallbackToPlainRest(IOException("HTTP 401")))
        assertFalse(shouldFallbackToPlainRest(IOException("HTTP 403")))
        assertFalse(shouldFallbackToPlainRest(IOException("TLS handshake failed")))
        assertFalse(shouldFallbackToPlainRest(IllegalArgumentException("Private DNS blocked")))
    }

    @Test fun disallowsCrossDomainFeeds() {
        val site = SiteUrl.parse("https://primjer.hr")
        assertThrows(IllegalArgumentException::class.java) {
            site.approved("https://nepoznato.hr/wp-content/uploads/feed.csv")
        }
        assertEquals(null, site.publicLink("http://primjer.hr/cjenik"))
    }
}
