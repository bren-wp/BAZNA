<?php
/**
 * Public legal metadata shared by BAZNA WordPress, Android and Desktop.
 *
 * Source: enacted NN 132/2026 (1583 and 1584) effective 2026-11-17.
 * NN 132/2026 (1578-1580) repeals former direct-control price-list decisions.
 *
 * @package Brendigo_Sidrena_Cijena
 */
defined( 'ABSPATH' ) || exit;

/**
 * This metadata is informative. It never overwrites historical price data.
 */
final class Brendigo_Sidrena_Cijena_Legal {
    const RULES_REVISION = 'NN-132-2026-1583-1584';
    const REGULATION_STATUS = 'enacted';
    const CONSULTATION_END = '2026-11-13';
    const EFFECTIVE_DATE = '2026-11-17';
    const REFERENCE_FMCG = '2025-05-02';
    const REFERENCE_GENERAL = '2026-09-10';
    const ARCHIVE_MIN_DAYS = 15;
    const PRICE_REDUCTION_LOOKBACK_DAYS = 30;
    const UNIT_PRICE_RULE_EFFECTIVE_FROM = '2026-09-26';
    const PUBLICATION_DEADLINE = '11:00';
    const CONSUMER_LAW_REVISION = 'NN-59-2026';
    const UNIT_PRICE_RULE_REVISION = 'NN-105-2026';

    /**
     * Stable, public, versioned contract. No customer details or credentials.
     *
     * @return array<string,mixed>
     */
    public static function public_contract() {
        return array(
            'schema' => 1,
            'reviewed_on' => '2026-11-17',
            'jurisdiction' => 'HR',
            'rules_revision' => self::RULES_REVISION,
            'consumer_law_revision' => self::CONSUMER_LAW_REVISION,
            'unit_price_rule_revision' => self::UNIT_PRICE_RULE_REVISION,
            'unit_price_rule_effective_from' => self::UNIT_PRICE_RULE_EFFECTIVE_FROM,
            'price_reduction_lookback_days' => self::PRICE_REDUCTION_LOOKBACK_DAYS,
            'discount_baseline_is_anchor_price' => false,
            // Enacted under NN 132/2026; actual merchant price evidence is NOT certified.
            'base_price_regulation_status' => self::REGULATION_STATUS,
            'base_price_rule_effective_from' => self::EFFECTIVE_DATE,
            'base_price_is_automatically_derived' => false,
            'historical_price_verification_required' => true,
            'legal_compliance_certified' => false,
            'consumer_30_day_low_is_anchor_price' => false,
            'consumer_30_day_low_is_base_price' => false,
            'draft_rulebooks_consultation_until' => self::CONSULTATION_END,
            'final_rulebooks_verified' => true,
            // The enacted legal rules are active; catalog compliance still requires verification.
            'implementation' => array(
                'published_decisions_ready_from' => self::EFFECTIVE_DATE,
                'reference_price_data_entry' => 'verified_manual',
                'product_service_publication' => 'csv_xml_html',
                'publication_source_of_truth' => 'wordPress_published_feeds',
                'new_rules_effective' => true,
                'unpublished_draft_features' => 'not_automatically_enabled',
                'discount_30_day_low_independent' => true,
                'automatic_historical_inference' => false,
            ),
            'effective_from' => self::EFFECTIVE_DATE,
            'reference_dates' => array(
                'legacy_fmcg' => self::REFERENCE_FMCG,
                'fmcg' => self::REFERENCE_FMCG,
                'general_goods_services' => self::REFERENCE_GENERAL,
            ),
            'legacy_categories' => array(
                'hrana', 'pice', 'kozmetika', 'sredstva_za_ciscenje',
                'toaletne_potrepstine', 'proizvodi_za_kucanstvo',
            ),
            'product_publication_deadline' => self::PUBLICATION_DEADLINE,
            'service_change_deadline' => self::PUBLICATION_DEADLINE,
            'timezone' => 'Europe/Zagreb', // Croatian regulatory deadline, regardless of WordPress site timezone.
            'archive_min_days' => self::ARCHIVE_MIN_DAYS,
            'archive_retention_anchor' => 'replacement_time',
            'price_list_update_deadline_local' => self::PUBLICATION_DEADLINE,
            'advertising_print_tv_base_price_required' => true,
            'reference_fmcg_discount_method' => 'lowest_price_in_30_days_before_discount_start',
            'reference_general_discount_method' => 'previous_retail_price_before_discount',
            'formats' => array( 'csv', 'xml' ),
            'products_required_fields' => array(
                'naziv', 'sku', 'marka', 'neto_kolicina', 'jedinica_mjere',
                'maloprodajna_cijena', 'poseban_oblik_prodaje',
                'cijena_jedinice_mjere', 'barkod', 'ean_barkod', 'bazna_cijena',
            ),
            'services_required_fields' => array(
                'naziv', 'maloprodajna_cijena', 'poseban_oblik_prodaje', 'bazna_cijena',
            ),
            'publication_filename_fields' => array( 'identity', 'outlet_or_all', 'goods_or_services', 'sequence', 'YYYYMMDD_HHMM' ),
            'classification' => 'manual_verified',
            'historical_prices' => 'not_inferred',
            'source_urls' => array(
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1578.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1579.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1580.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1212.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1213.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1309.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1310.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_06_59_728.html',
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html',
                'https://mingo.gov.hr/vijesti/ministar-susnjar-kroz-dijalog-s-gospodarstvenicima-do-provedivih-pravila-i-vece-zastite-potrosaca/10461',
            ),
        );
    }

    /**
     * Resolve only an explicit user-verified classification; never infer
     * from WooCommerce categories or assign a reference price automatically.
     *
     * @param string $scope legacy_fmcg|general_goods_services
     * @return string ISO date or empty string on unverified input.
     */
    public static function phase_on( $date ) {
        // Validate calendar dates, never infer legal force from a draft or a
        // server clock in the wrong timezone. Caller supplies local HR date.
        if ( ! is_string( $date ) || ! preg_match( '/^(\\d{4})-(\\d{2})-(\\d{2})$/D', $date, $parts ) ||
            ! checkdate( (int) $parts[2], (int) $parts[3], (int) $parts[1] ) ) {
            return 'unknown';
        }
        return $date < self::EFFECTIVE_DATE ? 'preparation' : 'rules_effective';
    }

    /**
     * NN 132/2026 (1583), article 3: choose the documentary method, never
     * create or certify a historical price. A first-offer date is permissible
     * only AFTER the scope-specific reference date.
     *
     * @return array{status:string,reference_date:string,method:string}
     */
    public static function reference_requirement( $scope, $first_offered_on = '', $discounted_on_reference = false ) {
        $date = self::reference_date( $scope );
        if ( '' === $date ) {
            return array( 'status' => 'requires_review', 'reference_date' => '', 'method' => '' );
        }
        if ( '' !== $first_offered_on ) {
            if ( ! is_string( $first_offered_on ) ||
                ! preg_match( '/^(\\d{4})-(\\d{2})-(\\d{2})$/D', $first_offered_on, $parts ) ||
                ! checkdate( (int) $parts[2], (int) $parts[3], (int) $parts[1] ) ||
                $first_offered_on <= $date ) {
                return array( 'status' => 'requires_review', 'reference_date' => '', 'method' => '' );
            }
            $date = $first_offered_on;
        }
        $method = 'retail_price_on_reference_day';
        if ( true === $discounted_on_reference ) {
            $method = 'legacy_fmcg' === $scope
                ? 'lowest_price_in_30_days_before_discount_start'
                : 'previous_retail_price_before_discount';
        }
        return array( 'status' => 'evidence_required', 'reference_date' => $date, 'method' => $method );
    }

    public static function reference_date( $scope ) {
        if ( 'legacy_fmcg' === $scope ) {
            return self::REFERENCE_FMCG;
        }
        if ( 'general_goods_services' === $scope ) {
            return self::REFERENCE_GENERAL;
        }
        return '';
    }
}
