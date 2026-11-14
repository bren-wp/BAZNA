package com.brendigo.bazna.ui

/**
 * Screen-width rules for the native SIDRENA shell, in Android dp.
 *
 * A phone keeps the 1:1 reference proportions. Tablets center the reading
 * column instead of stretching price cards to an ultra-wide monitor.
 * Compact bottom navigation keeps all four destinations tappable at 240dp.
 */
internal object AdaptiveSizing {
    const val MAX_CONTENT_WIDTH_DP = 840
    const val MAX_CONNECTION_WIDTH_DP = 560
    const val ICON_ONLY_NAV_BELOW_DP = 360

    fun contentWidthDp(availableDp: Int, connecting: Boolean = false): Int {
        val bound = if (connecting) MAX_CONNECTION_WIDTH_DP else MAX_CONTENT_WIDTH_DP
        return availableDp.coerceAtLeast(1).coerceAtMost(bound)
    }

    fun iconOnlyNavigation(availableDp: Int, fontScale: Float = 1f): Boolean =
        availableDp < ICON_ONLY_NAV_BELOW_DP ||
            (fontScale >= 1.3f && availableDp < 480)
}
