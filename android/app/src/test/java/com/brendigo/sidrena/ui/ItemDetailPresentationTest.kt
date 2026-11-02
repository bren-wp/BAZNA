package com.brendigo.sidrena.ui

import com.brendigo.sidrena.data.PriceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemDetailPresentationTest {
    private fun item(
        current: String = "5,99",
        reference: String = "8,49",
        referenceDate: String = "2026-10-01",
        availability: String = "Dostupno",
        kind: String = "proizvod",
    ) = PriceItem(
        id = "MU-001",
        name = "Maslinovo ulje",
        kind = kind,
        currentPrice = current,
        referencePrice = reference,
        referenceDate = referenceDate,
        sku = "MU-001",
        availability = availability,
        unit = "1 L",
    )

    @Test fun displaysPublishedPriceWithoutInventingDiscountOrPeriod() {
        val presentation = detailPresentation(item())
        assertTrue(presentation.currentPrice.startsWith("5,99"))
        assertTrue(presentation.referencePrice?.startsWith("8,49") == true)
        assertEquals("2026-10-01", presentation.referenceDate)
        assertFalse(presentation.currentPrice.contains("popust", ignoreCase = true))
        assertFalse(presentation.currentPrice.contains("od ", ignoreCase = true))
    }

    @Test fun doesNotDisplayReferenceDateWhenReferencePriceIsNotPublished() {
        val state = detailPresentation(item(reference = "", referenceDate = "2026-10-01"))
        assertNull(state.referencePrice)
        assertNull(state.referenceDate)
    }

    @Test fun missingCurrentPriceIsClearlyLabeled() {
        val state = detailPresentation(item(current = ""))
        assertFalse(state.hasCurrentPrice)
        assertEquals("Cijena nije objavljena", state.currentPrice)
    }

    @Test fun statusTextMustRemainExactlyAsPublished() {
        val state = detailPresentation(item(availability = "Nedostupno"))
        assertEquals("Nedostupno", state.availability)
    }

    @Test fun serviceTitleCanBeDistinguishedFromProduct() {
        assertTrue(detailPresentation(item(kind = "usluga")).isService)
        assertFalse(detailPresentation(item(kind = "proizvod")).isService)
    }

    @Test fun savesAndRestoresSelectedItemAcrossRotation() {
        val original = item(referenceDate = "10. 9. 2026.")
        val fields = saveDetailItem(original)
        assertEquals(14, fields.size)
        assertEquals(original, restoreDetailItem(fields))
        assertNull(restoreDetailItem(emptyList()))
        assertNull(restoreDetailItem(listOf("broken")))
    }

    @Test fun legacySavedDetailsRemainRestorableWithoutNewApiFields() {
        val older = saveDetailItem(item()).take(9)
        val oldItem = restoreDetailItem(older)
        assertEquals("", oldItem?.unitPrice)
        assertEquals("", oldItem?.saleLabel)
        assertEquals("", oldItem?.imageUrl)
    }

    @Test fun legalUnitPriceAndSaleLabelAreDisplayedOnlyWhenPublished() {
        val priced = item().copy(
            unitPrice = "3,99",
            saleLabel = "akcija",
            brand = "Brendigo",
            barcode = "3851234567891",
            imageUrl = "https://primjer.hr/img.webp",
        )
        val display = detailPresentation(priced)
        assertTrue(display.unitPrice?.startsWith("3,99") == true)
        assertEquals("akcija", display.saleLabel)
        assertEquals("Brendigo", display.brand)
        assertEquals("3851234567891", display.barcode)
        assertEquals(priced, restoreDetailItem(saveDetailItem(priced)))
        val empty = detailPresentation(priced.copy(unitPrice = "", saleLabel = ""))
        assertNull(empty.unitPrice)
        assertNull(empty.saleLabel)
    }

    @Test fun referenceIdentityCardUsesPublishedNameAndUnitOnce() {
        assertEquals("Maslinovo ulje 1 L", detailHeadline("Maslinovo ulje", "1 L"))
        assertEquals("Maslinovo ulje 1 L", detailHeadline("Maslinovo ulje 1 L", "1 L"))
        assertEquals("Usluga čišćenja", detailHeadline("Usluga čišćenja", ""))
        assertEquals("Sapun", detailHeadline("Sapun", null))
    }

    @Test fun missingReferenceDateIsUnknownNotAutomaticallyAssigned() {
        val state = detailPresentation(item(reference = "4,50", referenceDate = ""))
        assertTrue(state.referencePrice?.contains("4,50") == true)
        assertNull(state.referenceDate)
    }

    @Test fun absentUnpublishedDetailsAreNotInvented() {
        val initial = item().copy(name = "", sku = "", unit = "", availability = "")
        val result = detailPresentation(initial)
        assertEquals("Bez naziva", result.title)
        assertNull(result.sku)
        assertNull(result.unit)
        assertNull(result.availability)
    }
}
