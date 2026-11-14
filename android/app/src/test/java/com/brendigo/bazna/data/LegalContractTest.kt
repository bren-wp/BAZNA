package com.brendigo.bazna.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.json.JSONObject

class LegalContractTest {
    @Test fun legalConstantsMatchCroatianRules() {
        assertEquals("2026-11-17", LegalContract.EFFECTIVE)
        assertEquals("2025-05-02", LegalContract.LEGACY)
        assertEquals("2026-09-10", LegalContract.GENERAL)
        assertEquals("2026-09-26", LegalContract.UNIT_PRICE_EFFECTIVE)
    }

    @Test fun oldPluginIsSupportedWithoutFabricatedLegalStatus() {
        assertNull(LegalContract.parseOptional(null))
    }

    @Test fun parseValidatedServerProfile() {
        val json=JSONObject("""{"schema":1,"jurisdiction":"HR",
          "rules_revision":"NN-101-2026-NN-110-2026",
          "consumer_law_revision":"NN-59-2026",
          "unit_price_rule_revision":"NN-105-2026",
          "price_reduction_lookback_days":30,
          "discount_baseline_is_anchor_price":false,
          "unit_price_rule_effective_from":"2026-09-26",
          "effective_from":"2026-11-17",
          "reference_dates":{"legacy_fmcg":"2025-05-02",
             "general_goods_services":"2026-09-10"},
          "archive_min_days":30,"product_publication_deadline":"08:00"}""")
        val policy=LegalContract.parseOptional(json)
        assertEquals("2026-11-17", policy?.effectiveFrom)
        assertEquals(30, policy?.archiveMinDays)
        assertEquals("NN-59-2026", policy?.consumerLawRevision)
        assertEquals("NN-105-2026", policy?.unitPriceRuleRevision)
        assertEquals(30, policy?.priceReductionLookbackDays)
        assertEquals(false, policy?.discountBaselineIsAnchorPrice)
        assertEquals("2026-09-26", policy?.unitPriceRuleEffectiveFrom)
        val prepared = JSONObject(json.toString()).put("implementation", JSONObject().apply {
            put("reference_price_data_entry", "verified_manual")
            put("unpublished_draft_features", "not_automatically_enabled")
            put("discount_30_day_low_independent", true)
            put("published_decisions_ready_from", "2026-11-17")
        })
        assertEquals(
            "2026-11-17",
            LegalContract.parseOptional(prepared)?.implementationReadyFrom,
        )
        val closed = JSONObject(json.toString())
            .put("base_price_regulation_status", "consultation_closed_pending_final_text")
            .put("final_rulebooks_verified", false)
        assertEquals(
            "consultation_closed_pending_final_text",
            LegalContract.parseOptional(closed)?.basePriceRegulationStatus,
        )
        assertThrows(IllegalArgumentException::class.java) {
            LegalContract.parseOptional(closed.put("final_rulebooks_verified", true))
        }
        assertEquals(
            "draft_consultation",
            LegalContract.parseOptional(JSONObject(json.toString())
                .put("base_price_regulation_status", "draft_consultation"))
                ?.basePriceRegulationStatus,
        )
        prepared.getJSONObject("implementation")
            .put("discount_30_day_low_independent", false)
        assertThrows(IllegalArgumentException::class.java) {
            LegalContract.parseOptional(prepared)
        }
        assertEquals(
            "preparation",
            policy?.publishedDecisionPhaseOn(java.time.LocalDate.of(2026, 11, 16)),
        )
        assertEquals(
            "published_decisions_effective",
            policy?.publishedDecisionPhaseOn(java.time.LocalDate.of(2026, 11, 17)),
        )
        assertThrows(IllegalArgumentException::class.java) {
            LegalContract.parseOptional(JSONObject(json.toString()).put("discount_baseline_is_anchor_price", true))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LegalContract.parseOptional(JSONObject(json.toString()).put("price_reduction_lookback_days", 5))
        }
        val invalidRevision = JSONObject(json.toString()).put("unit_price_rule_revision", "script:value")
        assertThrows(IllegalArgumentException::class.java) { LegalContract.parseOptional(invalidRevision) }
        json.put("archive_min_days", 3)
        assertThrows(IllegalArgumentException::class.java) { LegalContract.parseOptional(json) }
    }
}
