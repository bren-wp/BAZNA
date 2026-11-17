package com.brendigo.bazna.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundRefreshPolicyTest {
    private fun due(now: Long, last: Long, connected: Boolean = true,
                    loading: Boolean = false, editing: Boolean = false) =
        ForegroundRefreshPolicy.shouldRefresh(now, last, connected, loading, editing)

    @Test fun respectsStalenessWithoutBackgroundPolling() {
        val t = ForegroundRefreshPolicy.MIN_INTERVAL_MS
        assertFalse(due(t + 999, 1000))
        assertTrue(due(t + 1000, 1000))
        assertTrue(due(t + 2001, 1000))
    }

    @Test fun neverRefreshesUnconnectedOrBusyUI() {
        val t = ForegroundRefreshPolicy.MIN_INTERVAL_MS
        assertFalse(due(t + 1000, 1000, connected = false))
        assertFalse(due(t + 1000, 1000, loading = true))
        assertFalse(due(t + 1000, 1000, editing = true))
        assertFalse(due(t + 1000, 0))
        assertFalse(due(100, 1000))
    }
}
