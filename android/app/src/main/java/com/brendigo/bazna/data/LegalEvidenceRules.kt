package com.brendigo.bazna.data

import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId

/**
 * NN 132/2026 reference decision parity with WP and the PHP Core.
 *
 * Read-only Android never claims a price is published or legally certified.
 * This validates structured evidence supplied by a trusted editor, not the
 * authenticity of historic documents, and cannot generate past prices.
 */
object LegalEvidenceRules {
    private const val FMCG = "legacy_fmcg"
    private const val GENERAL = "general_goods_services"

    fun issue(
        kind: String,
        scope: String?,
        method: String?,
        isoDate: String?,
        hasHistoricalSource: Boolean,
        verified: Boolean,
        today: LocalDate = LocalDate.now(ZoneId.of("Europe/Zagreb")),
    ): String? {
        if (!verified) return "historical_verified"
        if (!hasHistoricalSource) return "historical_source"
        if (kind != "proizvodi" && kind != "usluge") return "vrsta"
        if ((scope != FMCG && scope != GENERAL) || (kind == "usluge" && scope == FMCG)) {
            return "reference_scope"
        }
        val reference = if (scope == FMCG) LocalDate.of(2025, 5, 2) else LocalDate.of(2026, 9, 10)
        val date = try {
            if (isoDate == null || !Regex("""\d{4}-\d{2}-\d{2}""").matches(isoDate)) null
            else LocalDate.parse(isoDate)
        } catch (_: DateTimeException) {
            null
        } ?: return "sidrena_datum"
        if (date.isBefore(reference) || date.isAfter(today)) return "sidrena_datum"
        return when {
            method == "reference_day" && date == reference -> null
            method == "first_offer" && date.isAfter(reference) -> null
            method == "discount_lowest_30d" && scope == FMCG -> null
            method == "discount_previous_retail" && scope == GENERAL -> null
            else -> "historical_method"
        }
    }
}
