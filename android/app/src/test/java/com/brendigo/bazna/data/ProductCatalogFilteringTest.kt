package com.brendigo.bazna.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Screen 04 filtering uses only publicly available product data. */
class ProductCatalogFilteringTest {
    private val products = listOf(
        PriceItem("oil", "Maslinovo ulje", "proizvod", "5,99", "8,49",
            "2026-09-10", "MU-001", "Dostupno", "1 L"),
        PriceItem("pasta", "Tjestenina", "proizvod", "1,29", "",
            "", "TJ-002", "", "500 g"),
        PriceItem("juice", "Sok naranča", "proizvod", "2,49", "3,49",
            "2026-09-10", "SN-003", "", "1 L"),
    )

    @Test
    fun emptySearchShowsAllPublishedItemsInOriginalOrder() {
        assertEquals(listOf("oil", "pasta", "juice"),
            CatalogSearch.items(products, "").map { it.id })
    }

    @Test
    fun anchorFilterShowsOnlyItemsWithPublishedReferencePrices() {
        assertEquals(listOf("oil", "juice"),
            CatalogSearch.items(products, "", 1).map { it.id })
        assertEquals(listOf("pasta"),
            CatalogSearch.items(products, "", 2).map { it.id })
    }

    @Test
    fun unicodeNameAndProductCodeAreSearchableCaseInsensitively() {
        assertEquals("juice",
            CatalogSearch.items(products, "NARANČA").single().id)
        assertEquals("oil",
            CatalogSearch.items(products, "mu-001").single().id)
    }

    @Test
    fun searchAndAnchorFilterWorkTogether() {
        assertTrue(CatalogSearch.items(products, "tjestenina", 1).isEmpty())
        assertEquals("pasta",
            CatalogSearch.items(products, "tjestenina", 2).single().id)
    }

    @Test
    fun missingAnchorDoesNotBecomePromotionalPrice() {
        val pasta = CatalogSearch.items(products, "tjestenina").single()
        assertTrue(pasta.referencePrice.isBlank())
        assertEquals("1,29", pasta.currentPrice)
    }
}
