<?php
/**
 * Public legal metadata shared by WordPress, Android and SIDRENA Desktop.
 *
 * Source: NN 101/2026 (1212 and 1213), amended by NN 110/2026
 * (1309 and 1310). The proposed base-price and public-price-list rulebooks were still in
 * e-consultation on 2026-11-02 (until 2026-11-13), not enacted law.
 *
 * @package Brendigo_Sidrena_Cijena
 */
defined( 'ABSPATH' ) || exit;

/**
 * This metadata is informative. It never overwrites historical price data.
 */
final class Brendigo_Sidrena_Cijena_Legal {
    const RULES_REVISION = 'NN-101-2026-NN-110-2026';
    const EFFECTIVE_DATE = '2026-11-17';
    const REFERENCE_FMCG = '2025-05-02';
    const REFERENCE_GENERAL = '2026-09-10';
    const ARCHIVE_MIN_DAYS = 30;
    const PRICE_REDUCTION_LOOKBACK_DAYS = 30;
    const UNIT_PRICE_RULE_EFFECTIVE_FROM = '2026-09-26';
    const PUBLICATION_DEADLINE = '08:00';
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
            'reviewed_on' => '2026-11-02',
            'jurisdiction' => 'HR',
            'rules_revision' => self::RULES_REVISION,
            'consumer_law_revision' => self::CONSUMER_LAW_REVISION,
            'unit_price_rule_revision' => self::UNIT_PRICE_RULE_REVISION,
            'unit_price_rule_effective_from' => self::UNIT_PRICE_RULE_EFFECTIVE_FROM,
            'price_reduction_lookback_days' => self::PRICE_REDUCTION_LOOKBACK_DAYS,
            'discount_baseline_is_anchor_price' => false,
            // Informational only: the new NN 59/2026 price-display provisions
            // begin 17 Nov; final base-price method is still in consultation.
            'base_price_regulation_status' => 'draft_consultation',
            'base_price_rule_effective_from' => self::EFFECTIVE_DATE,
            'base_price_is_automatically_derived' => false,
            'historical_price_verification_required' => true,
            'legal_compliance_certified' => false,
            'consumer_30_day_low_is_anchor_price' => false,
            'consumer_30_day_low_is_base_price' => false,
            'draft_rulebooks_consultation_until' => '2026-11-13',
            'effective_from' => self::EFFECTIVE_DATE,
            'reference_dates' => array(
                'legacy_fmcg' => self::REFERENCE_FMCG,
                'general_goods_services' => self::REFERENCE_GENERAL,
            ),
            'legacy_categories' => array(
                'hrana', 'pice', 'kozmetika', 'sredstva_za_ciscenje',
                'toaletne_potrepstine', 'proizvodi_za_kucanstvo',
            ),
            'product_publication_deadline' => self::PUBLICATION_DEADLINE,
            'service_change_deadline' => self::PUBLICATION_DEADLINE,
            'timezone' => function_exists( 'wp_timezone_string' ) ? wp_timezone_string() : 'Europe/Zagreb',
            'archive_min_days' => self::ARCHIVE_MIN_DAYS,
            'formats' => array( 'csv', 'xml' ),
            'products_required_fields' => array(
                'naziv', 'sku', 'marka', 'jedinica_mjere',
                'cijena_jedinice_mjere', 'maloprodajna_cijena',
                'naziv_posebnog_oblika', 'sidrena_cijena', 'barkod', 'dostupnost',
            ),
            'services_required_fields' => array(
                'naziv', 'maloprodajna_cijena',
                'naziv_posebnog_oblika', 'sidrena_cijena',
            ),
            'classification' => 'manual_verified',
            'historical_prices' => 'not_inferred',
            'source_urls' => array(
                'https://narodne-novine.nn.hr/clanci/sluzbeni/2025_05_75_979.html',
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
