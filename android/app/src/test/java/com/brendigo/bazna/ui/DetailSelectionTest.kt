package com.brendigo.bazna.ui

import com.brendigo.bazna.data.CatalogIndex
import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.PriceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Restored detail values must always be read from the active public feed. */
class DetailSelectionTest {
    private val old = PriceItem(
        id = "42", name = "Proizvod", kind = "proizvod",
        currentPrice = "5,00", referencePrice = "8,00",
        referenceDate = "2026-09-10", sku = "SKU-42",
        availability = "Dostupno", unit = "kom",
    )

    private fun catalog(vararg items: PriceItem, site: String = "https://shop.example"): CatalogSnapshot =
        CatalogSnapshot(
            site = site,
            index = CatalogIndex(
                publishedAt = null,
                publicPage = null,
                archivePage = null,
                productsCsv = "",
                servicesCsv = "",
                outlets = emptyList(),
            ),
            products = items.filter { it.kind == "proizvod" },
            services = items.filter { it.kind == "usluga" },
            archive = emptyList(),
            warnings = emptyList(),
        )

    @Test fun restoredSelectionUsesNewlyPublishedPriceNotSavedPrice() {
        val refreshed = old.copy(currentPrice = "6,20", referencePrice = "7,90")
        assertEquals(
            refreshed,
            resolveCurrentDetailItem(old, catalog(refreshed), "shop-1", "shop-1", "https://shop.example"),
        )
    }

    @Test fun mismatchedOutletCannotShowAnotherOutletsPrices() {
        assertNull(
            resolveCurrentDetailItem(old, catalog(old), "shop-1", "", "https://shop.example"),
        )
    }

    @Test fun mismatchedSiteCannotShowOldSitesPrices() {
        assertNull(
            resolveCurrentDetailItem(old, catalog(old, site = "https://new.example"), "", "", "https://shop.example"),
        )
    }

    @Test fun deletedOrRenamedItemIsNotDisplayedAsCurrent() {
        assertNull(
            resolveCurrentDetailItem(old, catalog(old.copy(id = "99")), "", "", "https://shop.example"),
        )
    }

    @Test fun serviceDoesNotResolveToProductWithSameId() {
        val service = old.copy(kind = "usluga", sku = "")
        assertNull(
            resolveCurrentDetailItem(service, catalog(old), "", "", "https://shop.example"),
        )
    }

    @Test fun itemWithMissingIdCanUseNonemptySku() {
        val saved = old.copy(id = "")
        val live = saved.copy(currentPrice = "7,10")
        assertEquals(
            live,
            resolveCurrentDetailItem(saved, catalog(live), "", "", "https://shop.example"),
        )
        assertNull(
            resolveCurrentDetailItem(saved.copy(sku = ""), catalog(live), "", "", "https://shop.example"),
        )
    }

    @Test fun missingSnapshotItemIsNeverReconstructedFromStaleFields() {
        assertNull(resolveCurrentDetailItem(old, catalog(), "", "", "https://shop.example"))
        assertNull(resolveCurrentDetailItem(null, catalog(old), "", "", "https://shop.example"))
    }

    @Test fun skuChangeInvalidatesSavedIdentity() {
        assertNull(resolveCurrentDetailItem(
            old, catalog(old.copy(sku = "DIFFERENT")), "", "", "https://shop.example",
        ))
    }
}
