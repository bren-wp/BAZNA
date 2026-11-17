package com.brendigo.bazna.data

/**
 * Foreground-only publication revalidation. No WorkManager, alarm or background polling.
 * Uses a monotonic clock, so timezone changes cannot cause extra requests.
 */
internal object ForegroundRefreshPolicy {
    const val MIN_INTERVAL_MS = 2 * 60 * 1000L

    fun shouldRefresh(
        nowElapsedMs: Long,
        lastSuccessfulLoadElapsedMs: Long,
        hasSnapshot: Boolean,
        loading: Boolean,
        editingSite: Boolean,
    ): Boolean = hasSnapshot && !loading && !editingSite &&
        lastSuccessfulLoadElapsedMs > 0L &&
        nowElapsedMs >= lastSuccessfulLoadElapsedMs &&
        nowElapsedMs - lastSuccessfulLoadElapsedMs >= MIN_INTERVAL_MS
}
