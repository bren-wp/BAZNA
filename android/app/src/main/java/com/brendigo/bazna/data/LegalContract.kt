package com.brendigo.bazna.data

import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** The public WordPress legal profile is advisory; no inferred historical prices. */
data class LegalContract(
    val rulesRevision: String,
    val effectiveFrom: String,
    val legacyReferenceDate: String,
    val generalReferenceDate: String,
    val archiveMinDays: Int,
    val dailyDeadline: String,
    val consumerLawRevision: String? = null,
    val unitPriceRuleRevision: String? = null,
    val priceReductionLookbackDays: Int? = null,
    val discountBaselineIsAnchorPrice: Boolean? = null,
    val unitPriceRuleEffectiveFrom: String? = null,
    val basePriceRegulationStatus: String? = null,
    val basePriceRuleEffectiveFrom: String? = null,
    val historicalPriceVerificationRequired: Boolean? = null,
    val legalComplianceCertified: Boolean? = null,
    val implementationReadyFrom: String? = null,
) {
    /**
     * The publication decisions' scheduled application date, NOT evidence
     * that the draft rulebooks were enacted or a merchant is compliant.
     */
    fun publishedDecisionPhaseOn(localDate: LocalDate): String =
        if (localDate.isBefore(LocalDate.parse(effectiveFrom))) "preparation"
        else "published_decisions_effective"

    companion object {
        const val REVISION = "NN-101-2026-NN-110-2026"
        const val EFFECTIVE = "2026-11-17"
        const val LEGACY = "2025-05-02"
        const val GENERAL = "2026-09-10"
        const val CONSUMER_LAW = "NN-59-2026"
        const val UNIT_PRICE_RULE = "NN-105-2026"
        const val UNIT_PRICE_EFFECTIVE = "2026-09-26"

        fun parseOptional(json: JSONObject?): LegalContract? {
            if (json == null) return null // Old WordPress API version.
            require(json.optInt("schema", -1) == 1 && json.optString("jurisdiction") == "HR") {
                "Nepodržani pravni profil."
            }
            val revision = json.getString("rules_revision")
            require(revision.length in 5..80) { "Neispravno pravno izdanje." }
            val effective = iso(json.getString("effective_from"))
            val dates = json.getJSONObject("reference_dates")
            val legacy = iso(dates.getString("legacy_fmcg"))
            val general = iso(dates.getString("general_goods_services"))
            val days = json.getInt("archive_min_days")
            require(days in 30..3650) { "Neispravno čuvanje arhive." }
            val deadline = json.getString("product_publication_deadline")
            require(Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(deadline)) {
                "Neispravno vrijeme objave."
            }
            fun optionalRevision(field: String): String? {
                if (!json.has(field) || json.isNull(field)) return null
                val value = json.getString(field)
                require(Regex("NN-[0-9]{1,3}-[0-9]{4}").matches(value)) {
                    "Neispravna oznaka pravnog izvora."
                }
                return value
            }
            val discountDays = if (json.has("price_reduction_lookback_days")) {
                val v = json.get("price_reduction_lookback_days")
                require(v is Int && v == 30) { "Neispravno razdoblje prije sniženja." }
                v
            } else null
            val anchorBaseline = if (json.has("discount_baseline_is_anchor_price")) {
                val v = json.get("discount_baseline_is_anchor_price")
                require(v is Boolean && !v) { "Bazna cijena nije osnova za izračun popusta." }
                v
            } else null
            val unitEffective = if (json.has("unit_price_rule_effective_from")) {
                iso(json.getString("unit_price_rule_effective_from"))
            } else null
            fun optionalFalse(field: String): Boolean? {
                if (!json.has(field) || json.isNull(field)) return null
                val v = json.get(field)
                require(v is Boolean && !v) { "Neprovjerena pravna potvrda ili izvedena bazna cijena." }
                return v
            }
            fun optionalTrue(field: String): Boolean? {
                if (!json.has(field) || json.isNull(field)) return null
                val v = json.get(field)
                require(v is Boolean && v) { "Nedostaje obvezna potvrda povijesnog podatka." }
                return v
            }
            val baseStatus = if (json.has("base_price_regulation_status")) {
                json.getString("base_price_regulation_status").also {
                    require(it in setOf("draft_consultation", "consultation_closed_pending_final_text")) {
                        "Nepoznat status pravilnika o baznoj cijeni."
                    }
                }
            } else null
            // Optional: new WordPress versions explicitly report when final
            // rulebook texts have not yet been verified in Narodne novine.
            optionalFalse("final_rulebooks_verified")
            val baseEffective = if (json.has("base_price_rule_effective_from")) {
                iso(json.getString("base_price_rule_effective_from"))
            } else null
            optionalFalse("base_price_is_automatically_derived")
            optionalFalse("consumer_30_day_low_is_anchor_price")
            optionalFalse("consumer_30_day_low_is_base_price")
            // Optional rollout contract shared with the newer WordPress plugin.
            // Older versions remain readable. Do not accept a server that
            // claims an unverified draft was activated as binding law.
            val implementation = json.optJSONObject("implementation")
            val targetDate = implementation?.let { rollout ->
                require(rollout.optString("reference_price_data_entry") == "verified_manual") {
                    "Bazna cijena mora potjecati iz provjerene evidencije."
                }
                require(rollout.optString("unpublished_draft_features") == "not_automatically_enabled") {
                    "Neobjavljeni pravilnici ne smiju se primijeniti automatski."
                }
                require(rollout.opt("discount_30_day_low_independent") == true) {
                    "Najniža cijena prije sniženja mora ostati zasebna."
                }
                iso(rollout.getString("published_decisions_ready_from")).also {
                    require(it == effective) { "Nepodudarni datumi primjene." }
                }
            }
            return LegalContract(
                revision, effective, legacy, general, days, deadline,
                optionalRevision("consumer_law_revision"),
                optionalRevision("unit_price_rule_revision"),
                discountDays, anchorBaseline, unitEffective,
                baseStatus, baseEffective,
                optionalTrue("historical_price_verification_required"),
                optionalFalse("legal_compliance_certified"),
                targetDate,
            )
        }

        private fun iso(value: String): String {
            try {
                require(LocalDate.parse(value).toString() == value) { "Neispravan datum." }
                return value
            } catch (_: DateTimeParseException) {
                throw IllegalArgumentException("Neispravan datum u pravnom profilu.")
            }
        }
    }
}
