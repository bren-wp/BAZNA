package com.brendigo.bazna.ui

import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.PriceItem

/**
 * Never show a saved price as if it were the latest price after process death,
 * a catalog refresh, outlet change or site change. Restored data only carries
 * selection identity; the displayed price must come from the live CSV snapshot.
 */
internal fun resolveCurrentDetailItem(
    saved: PriceItem?,
    snapshot: CatalogSnapshot,
    savedOutlet: String,
    currentOutlet: String,
    savedSite: String,
): PriceItem? {
    if (saved == null || savedOutlet != currentOutlet || savedSite != snapshot.site) return null
    val candidates = when (saved.kind) {
        "usluga" -> snapshot.services
        "proizvod" -> snapshot.products
        else -> return null
    }
    if (saved.id.isBlank() && saved.sku.isBlank()) return null

    return candidates.firstOrNull { candidate ->
        candidate.kind == saved.kind &&
            if (saved.id.isNotBlank()) {
                candidate.id == saved.id &&
                    (saved.sku.isBlank() || candidate.sku == saved.sku)
            } else {
                candidate.sku.isNotBlank() && candidate.sku == saved.sku
            }
    }
}
