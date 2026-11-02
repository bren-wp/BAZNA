package com.brendigo.sidrena.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Screen 02 preserves HTTPS input behavior while matching the reference. */
class SidrenaConnectionLayoutTest {
    @Test
    fun compactAndTallLayoutsHavePositiveUsableSpacing() {
        val compact = connectionSpacing(640f)
        val tall = connectionSpacing(852f)
        assertEquals(14f, compact.top.value, 0.01f)
        assertEquals(24f, compact.intro.value, 0.01f)
        assertEquals(45f, compact.footer.value, 0.01f)
        assertEquals(24f, tall.top.value, 0.01f)
        assertEquals(48f, tall.intro.value, 0.01f)
        assertEquals(124f, tall.footer.value, 0.01f)
        assertTrue(tall.footer.value > compact.footer.value)
    }

    @Test
    fun realUserTypedHttpsValuePassesThroughUntouched() {
        val actual = "https://moja-trgovina.hr/podstranica"
        assertEquals(actual, acceptedSiteInput(actual))
        // Real validation, including HTTPS and domain rules, remains in SiteUrl.parse.
    }

    @Test
    fun newlinesCannotCreateMultilineOrSpoofedInput() {
        assertNull(acceptedSiteInput("https://primjer.hr\nhttps://evil.example"))
        assertNull(acceptedSiteInput("https://primjer.hr\r"))
    }

    @Test
    fun overlongValuesAreRejectedBeforeViewModel() {
        assertNull(acceptedSiteInput("h".repeat(2049)))
        assertEquals(2048, acceptedSiteInput("a".repeat(2048))?.length)
    }

    @Test
    fun placeholderIsNeverInjectedIntoTheRealInput() {
        assertEquals("", acceptedSiteInput(""))
        assertEquals(" ", acceptedSiteInput(" "))
    }
}
