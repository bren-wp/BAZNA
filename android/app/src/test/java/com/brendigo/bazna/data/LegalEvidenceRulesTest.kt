package com.brendigo.bazna.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LegalEvidenceRulesTest {
    private val date = LocalDate.of(2026, 11, 17)

    @Test fun legalMethodsMatchWordPressAndPhpCore() {
        data class Case(
            val kind: String,
            val scope: String,
            val method: String,
            val effectiveDate: String,
            val expected: String? = null,
        )
        val cases = listOf(
            Case("proizvodi", "legacy_fmcg", "reference_day", "2025-05-02"),
            Case("proizvodi", "legacy_fmcg", "first_offer", "2025-05-03"),
            Case("proizvodi", "legacy_fmcg", "discount_lowest_30d", "2025-05-02"),
            Case("usluge", "general_goods_services", "discount_previous_retail", "2026-09-10"),
            Case("proizvodi", "general_goods_services", "first_offer", "2026-10-01"),
            Case("proizvodi", "general_goods_services", "first_offer", "2026-09-10", "historical_method"),
            Case("proizvodi", "general_goods_services", "reference_day", "2025-05-02", "sidrena_datum"),
            Case("usluge", "legacy_fmcg", "reference_day", "2025-05-02", "reference_scope"),
            Case("proizvodi", "legacy_fmcg", "discount_previous_retail", "2025-05-02", "historical_method"),
            Case("usluge", "general_goods_services", "discount_lowest_30d", "2026-09-10", "historical_method"),
            Case("proizvodi", "general_goods_services", "first_offer", "2099-01-01", "sidrena_datum"),
        )
        for (case in cases) {
            assertEquals(case.toString(), case.expected, LegalEvidenceRules.issue(
                case.kind, case.scope, case.method, case.effectiveDate, true, true, date,
            ))
        }
    }

    @Test fun missingSourceAndAttestationNeverBecomeConfirmed() {
        assertEquals("historical_verified", LegalEvidenceRules.issue(
            "proizvodi", "legacy_fmcg", "reference_day", "2025-05-02", true, false, date,
        ))
        assertEquals("historical_source", LegalEvidenceRules.issue(
            "proizvodi", "legacy_fmcg", "reference_day", "2025-05-02", false, true, date,
        ))
    }
}
