<?php
/** Standalone legal contract regression test (no database required). */
define( 'ABSPATH', __DIR__ . '/' );
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-legal.php';

function sidrena_expect( $condition, $message ) {
    if ( ! $condition ) {
        fwrite( STDERR, 'FAIL: ' . $message . PHP_EOL );
        exit( 1 );
    }
}
$contract = Brendigo_Sidrena_Cijena_Legal::public_contract();
sidrena_expect( 1 === $contract['schema'], 'Versioned contract' );
sidrena_expect( 'HR' === $contract['jurisdiction'], 'Croatian jurisdiction' );
sidrena_expect( '2026-11-17' === $contract['effective_from'], 'Amended effective date' );
sidrena_expect( '2025-05-02' === $contract['reference_dates']['legacy_fmcg'], 'Legacy date' );
sidrena_expect( '2026-09-10' === $contract['reference_dates']['general_goods_services'], 'General date' );
sidrena_expect( 15 === $contract['archive_min_days'], 'Archive minimum' );
sidrena_expect( 30 === $contract['price_reduction_lookback_days'], 'Separate 30-day sale baseline' );
sidrena_expect( false === $contract['discount_baseline_is_anchor_price'], 'Anchor is not sale baseline' );
sidrena_expect( '2026-09-26' === $contract['unit_price_rule_effective_from'], 'NN105 effective date' );
sidrena_expect( '2026-11-13' === $contract['draft_rulebooks_consultation_until'], 'Drafts pending' );
sidrena_expect( '11:00' === $contract['product_publication_deadline'], 'Daily deadline' );
sidrena_expect( 'Europe/Zagreb' === $contract['timezone'], 'Publication time uses Croatian local timezone' );
sidrena_expect( true === $contract['final_rulebooks_verified'], 'Both final rulebooks published in NN 132/2026' );
sidrena_expect( '' === Brendigo_Sidrena_Cijena_Legal::reference_date( 'unknown' ), 'No inferred category' );
sidrena_expect( '2025-05-02' === Brendigo_Sidrena_Cijena_Legal::reference_date( 'legacy_fmcg' ), 'Verified legacy date' );
sidrena_expect( '2026-09-10' === Brendigo_Sidrena_Cijena_Legal::reference_date( 'general_goods_services' ), 'Verified new date' );
sidrena_expect( 'lowest_price_in_30_days_before_discount_start' === Brendigo_Sidrena_Cijena_Legal::reference_requirement( 'legacy_fmcg', '', true )['method'], 'NN132 art 3(2): FMCG discounted reference uses the 30-day low before that discount' );
sidrena_expect( 'previous_retail_price_before_discount' === Brendigo_Sidrena_Cijena_Legal::reference_requirement( 'general_goods_services', '', true )['method'], 'NN132 art 3(4): other goods/services discounted reference use prior retail price' );
sidrena_expect( '2025-06-03' === Brendigo_Sidrena_Cijena_Legal::reference_requirement( 'legacy_fmcg', '2025-06-03' )['reference_date'], 'NN132 art 3(1): first-offer FMCG date after 2025-05-02' );
sidrena_expect( '2026-09-11' === Brendigo_Sidrena_Cijena_Legal::reference_requirement( 'general_goods_services', '2026-09-11' )['reference_date'], 'NN132 art 3(1): first-offer general date after 2026-09-10' );
sidrena_expect( 'requires_review' === Brendigo_Sidrena_Cijena_Legal::reference_requirement( 'general_goods_services', '2025-07-03' )['status'], 'Disallow a general-goods first-offer date before the reference date' );
sidrena_expect( 'requires_review' === Brendigo_Sidrena_Cijena_Legal::reference_requirement( 'unknown', '', true )['status'], 'No legal inference without explicit classification' );
sidrena_expect( 'evidence_required' === Brendigo_Sidrena_Cijena_Legal::reference_requirement( 'legacy_fmcg', '', true )['status'], 'Method alone never certifies historical price' );
sidrena_expect( 'NN-59-2026' === $contract['consumer_law_revision'], 'Consumer law revision' );
sidrena_expect( '2026-11-17' === $contract['reviewed_on'], 'Legal profile reviewed on 14 Nov 2026' );
sidrena_expect( 'enacted' === $contract['base_price_regulation_status'], 'NN 132/2026 enacted' );
sidrena_expect( 'verified_manual' === $contract['implementation']['reference_price_data_entry'], 'Never infer a historical price' );
sidrena_expect( 'not_automatically_enabled' === $contract['implementation']['unpublished_draft_features'], 'Draft functionality not binding' );
sidrena_expect( true === $contract['implementation']['discount_30_day_low_independent'], '30-day baseline separate' );
sidrena_expect( 'preparation' === Brendigo_Sidrena_Cijena_Legal::phase_on( '2026-11-09' ), 'Before 17 Nov prepare only' );
sidrena_expect( 'rules_effective' === Brendigo_Sidrena_Cijena_Legal::phase_on( '2026-11-17' ), 'Published decision scheduled date' );
sidrena_expect( 'unknown' === Brendigo_Sidrena_Cijena_Legal::phase_on( '2026-11-31' ), 'Invalid dates rejected' );
sidrena_expect( '2026-11-17' === $contract['base_price_rule_effective_from'], 'New legal date is not earlier than 17 Nov' );
sidrena_expect( false === $contract['base_price_is_automatically_derived'], 'Base price is not reconstructed from anchor or current price' );
sidrena_expect( false === $contract['consumer_30_day_low_is_anchor_price'], '30-day sale baseline is separate from anchor' );
sidrena_expect( false === $contract['consumer_30_day_low_is_base_price'], '30-day sale baseline is separate from base price' );
sidrena_expect( true === $contract['historical_price_verification_required'], 'Historical price requires actual evidence' );
sidrena_expect( false === $contract['legal_compliance_certified'], 'Public profile must not certify site compliance' );

sidrena_expect( 'NN-105-2026' === $contract['unit_price_rule_revision'], 'Unit price regulation' );
sidrena_expect( in_array( 'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html', $contract['source_urls'], true ), 'Published unit-price rule source' );
sidrena_expect( in_array( 'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html', $contract['source_urls'], true ), 'Enacted publication rulebook source' );
sidrena_expect( 'NN-132-2026-1583-1584' === $contract['rules_revision'], 'Final rulebooks revision' );
sidrena_expect( 'replacement_time' === $contract['archive_retention_anchor'], 'Retention runs from replacement' );
sidrena_expect( array( 'naziv', 'maloprodajna_cijena', 'poseban_oblik_prodaje', 'bazna_cijena' ) === $contract['services_required_fields'], 'Current services export fields' );
sidrena_expect( ! empty( $contract['source_urls'] ), 'Legal sources' );
fwrite( STDOUT, "PASS: BAZNA legal contract\n" );
