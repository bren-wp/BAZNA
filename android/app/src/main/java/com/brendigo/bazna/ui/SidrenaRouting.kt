package com.brendigo.bazna.ui

/** Pure navigation policy so the Compose shell remains focused on rendering. */
internal fun shouldRecordSearchReturn(current: String, destination: String): Boolean =
    destination == "search" && current != "search" && current != "detail"

internal fun backDestination(
    current: String,
    detailReturn: String,
    searchReturn: String,
): String = when (current) {
    "detail" -> detailReturn
    "about" -> "settings"
    "search" -> searchReturn
    else -> "home"
}

internal fun bottomTabForPage(current: String, searchReturn: String): Int = when (current) {
    "home", "locations" -> 0
    "search" -> if (searchReturn == "home") 0 else 1
    "archive" -> 2
    "settings", "about" -> 3
    else -> 1
}

internal fun pageForBottomTab(index: Int): String = when (index) {
    0 -> "home"
    1 -> "cjenici"
    2 -> "archive"
    else -> "settings"
}
