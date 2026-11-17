<?php
/** NN 132/2026 official goods/services projection tests, runnable without WordPress. */
define( 'ABSPATH', __DIR__ . '/' );
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-official-feed.php';

function expect_official( $condition, $label ) {
	if ( ! $condition ) {
		fwrite( STDERR, "FAIL: $label\n" );
		exit( 1 );
	}
}

$goods = Brendigo_Sidrena_Cijena_Official_Feed::columns( 'proizvodi' );
expect_official(
	array( 'naziv', 'sku', 'marka', 'neto_kolicina', 'jedinica_mjere',
		'maloprodajna_cijena', 'poseban_oblik_prodaje', 'cijena_jedinice_mjere',
		'barkod', 'ean_barkod', 'bazna_cijena' ) === $goods,
	'Exact article 3(2) goods column order'
);
$services = Brendigo_Sidrena_Cijena_Official_Feed::columns( 'usluge' );
expect_official(
	array( 'naziv', 'maloprodajna_cijena', 'poseban_oblik_prodaje', 'bazna_cijena' ) === $services,
	'Exact article 3(3) services column order'
);
$row = array(
	'naziv' => 'Ulje 0,5 l', 'sku' => '0001', 'marka' => 'Primjer',
	'neto_kolicina' => '0,5', 'jedinica_mjere' => 'l',
	'maloprodajna_cijena' => '3.10', 'naziv_posebnog_oblika' => 'akcija',
	'cijena_jedinice_mjere' => '6.20', 'barkod' => '0123456789012',
	'ean_barkod' => '0123456789012', 'sidrena_cijena' => '3.90',
	'dostupnost' => 'dostupno', 'url' => 'https://example.com/',
	'historical_verified' => 'yes', 'reference_scope' => 'legacy_fmcg',
	'historical_method' => 'reference_day', 'sidrena_datum' => '02.05.2025.',
);
$projected = Brendigo_Sidrena_Cijena_Official_Feed::project( $row, 'proizvodi' );
expect_official( array_keys( $projected ) === $goods, 'No extra compatibility fields in official export' );
expect_official( $projected['bazna_cijena'] === '3.90', 'Historical base is retained' );
expect_official( $projected['poseban_oblik_prodaje'] === 'da', 'Special sale flag' );
expect_official( $projected['ean_barkod'] === '0123456789012', 'Leading EAN zeros retained' );
expect_official( array() === Brendigo_Sidrena_Cijena_Official_Feed::missing_required_data( array( $row ), 'proizvodi' ), 'Complete row accepted' );
$not_verified = $row;
$not_verified['historical_verified'] = '';
expect_official( 'historical_verified' === Brendigo_Sidrena_Cijena_Official_Feed::evidence_issue( $not_verified, 'proizvodi' ), 'No historic evidence: block official publication' );
// Regression: a manually/imported yes flag must not bypass missing proof.
$unproved = $row;
$unproved['historical_source'] = '   ';
expect_official( 'historical_source' === Brendigo_Sidrena_Cijena_Official_Feed::evidence_issue( $unproved, 'proizvodi' ), 'Yes flag without historical source is rejected at publication boundary' );
$proof_errors = Brendigo_Sidrena_Cijena_Official_Feed::missing_required_data( array( $unproved ), 'proizvodi' );
expect_official( count( $proof_errors ) === 1 && 'historical_source' === $proof_errors[0]['field'], 'Official feed refuses unproved imported historical price' );
$short_source = $row;
$short_source['historical_source'] = ' abc ';
expect_official( 'historical_source' === Brendigo_Sidrena_Cijena_Official_Feed::evidence_issue( $short_source, 'proizvodi' ), 'Short historical source cannot validate a confirmed price' );
$bad_scope = $row;
$bad_scope['reference_scope'] = 'general_goods_services';
expect_official( 'sidrena_datum' === Brendigo_Sidrena_Cijena_Official_Feed::evidence_issue( $bad_scope, 'proizvodi' ), 'A later general-goods reference cannot use 2025 date' );
$bad_method = $row;
$bad_method['historical_method'] = 'discount_previous_retail';
expect_official( 'historical_method' === Brendigo_Sidrena_Cijena_Official_Feed::evidence_issue( $bad_method, 'proizvodi' ), 'FMCG cannot use the other-goods discount method' );
$bad_services = $row;
expect_official( 'reference_scope' === Brendigo_Sidrena_Cijena_Official_Feed::evidence_issue( $bad_services, 'usluge' ), 'Services cannot use FMCG reference scope' );
$first_offer = $row;
$first_offer['historical_method'] = 'first_offer';
$first_offer['sidrena_datum'] = '03.05.2025.';
expect_official( '' === Brendigo_Sidrena_Cijena_Official_Feed::evidence_issue( $first_offer, 'proizvodi' ), 'First-offer date after applicable reference accepted with verified evidence' );

$incomplete = $row;
$incomplete['neto_kolicina'] = '';
expect_official( 1 === count( Brendigo_Sidrena_Cijena_Official_Feed::missing_required_data( array( $incomplete ), 'proizvodi' ) ), 'Net quantity checked when unit price is present' );
$incomplete['sidrena_cijena'] = '';
expect_official( 2 === count( Brendigo_Sidrena_Cijena_Official_Feed::missing_required_data( array( $incomplete ), 'proizvodi' ) ), 'Missing historic price cannot be inferred' );
expect_official( Brendigo_Sidrena_Cijena_Official_Feed::valid_ean( '0123456789012' ), 'EAN13 with leading zero' );
expect_official( Brendigo_Sidrena_Cijena_Official_Feed::valid_ean( '12345670' ), 'EAN8 checksum' );
expect_official( ! Brendigo_Sidrena_Cijena_Official_Feed::valid_ean( '0123456789011' ), 'Invalid EAN rejected' );
expect_official( Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678903', 'oib' ), '11-digit OIB with correct check digit' );
expect_official( ! Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678901', 'oib' ), 'Incorrect OIB check digit rejected' );
expect_official( Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678', 'maticni_broj' ), '8-digit MB identifier shape' );
expect_official( ! Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678', 'oib' ), 'Wrong identity kind rejected' );
expect_official( ! Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678901/../', 'oib' ), 'Filename traversal rejected' );

// Minimal WP stubs: legal filename tests must run standalone in CI.
class Brendigo_Sidrena_Cijena_Settings {
	public static $values = array(
		'trader_identifier' => '12345678903',
		'trader_identifier_kind' => 'oib',
		'file_oblik' => 'webshop',
		'file_adresa' => '',
		'file_oznaka' => '001',
	);
	public static $multi = true;
	public static function instance() { return new self(); }
	public function get( $key, $fallback = '' ) { return self::$values[ $key ] ?? $fallback; }
	public function normalize_oznaka( $text ) { return preg_replace( '/[^A-Za-z0-9-]/', '', (string) $text ); }
	public function is_multi_poslovnica() { return self::$multi; }
	public function sanitize_file_part( $value, $fallback = '' ) {
		$value = trim( preg_replace( '/[^A-Za-z0-9]+/', '_', (string) $value ), '_' );
		return '' === $value ? $fallback : $value;
	}
}
class Brendigo_Sidrena_Cijena_Poslovnice {
	public static function instance() { return new self(); }
	public function get_by_oznaka( $oznaka ) { return 'EXISTING' === $oznaka ?
		array( 'oblik' => 'trgovina', 'adresa' => 'Zagreb 1' ) : null; }
}
function wp_date( $format, $time, $timezone ) {
	return ( new DateTimeImmutable( '@' . $time ) )->setTimezone( $timezone )->format( $format );
}
$official = 'Brendigo_Sidrena_Cijena_Official_Feed';
expect_official( '' === $official::filename( 'proizvodi', 'csv', '', 1, 1794913200 ),
	'Missing webshop address cannot be silently replaced by a placeholder' );
expect_official( '' === $official::filename( 'proizvodi', 'csv', 'UNKNOWN', 1, 1794913200 ),
	'Unknown outlet cannot be substituted with webshop data' );
expect_official( str_contains( $official::filename( 'proizvodi', 'csv', 'EXISTING', 1, 1794913200 ),
	'trgovina_Zagreb_1_EXISTING_ROBA_1_' ), 'Known outlet requires exact address and identifier' );
Brendigo_Sidrena_Cijena_Settings::$values['file_adresa'] = 'Ulica 1';
expect_official( str_contains( $official::filename( 'usluge', 'xml', '', 3, 1794913200 ),
	'webshop_Ulica_1_001_USLUGE_3_' ), 'Configured webshop legal filename accepted' );
Brendigo_Sidrena_Cijena_Settings::$multi = false;
expect_official( str_contains( $official::filename( 'proizvodi', 'csv', '', 4, 1794913200 ),
	'ZA_SVA_PRODAJNA_MJESTA_ROBA_4_' ), 'All-outlets exception does not need fake outlet data' );

/**
 * Shared legal decisions: WordPress plugin and PHP Core, which also runs
 * inside Joomla, Drupal and the self-hosted web edition, must agree.
 * No historical source text is put into official published CSV or XML.
 */
require_once __DIR__ . '/../web/src/Core.php';
$cases = array(
	array( 'proizvodi', 'legacy_fmcg', 'reference_day', '2025-05-02', true, 'Arhiva 2025', '' ),
	array( 'proizvodi', 'legacy_fmcg', 'first_offer', '2025-05-03', true, 'Prva ponuda 2025', '' ),
	array( 'proizvodi', 'legacy_fmcg', 'discount_lowest_30d', '2025-05-02', true, 'Najniža prije početka popusta', '' ),
	array( 'usluge', 'general_goods_services', 'discount_previous_retail', '2026-09-10', true, 'Prethodna cijena 2026', '' ),
	array( 'proizvodi', 'general_goods_services', 'first_offer', '2026-10-01', true, 'Prva ponuda 2026', '' ),
	array( 'proizvodi', 'general_goods_services', 'first_offer', '2026-09-10', true, 'Krivi dan', 'historical_method' ),
	array( 'proizvodi', 'general_goods_services', 'first_offer', '2099-01-01', true, 'Izmišljeni budući datum', 'sidrena_datum' ),
	array( 'proizvodi', 'general_goods_services', 'reference_day', '2025-05-02', true, 'Krivi dan', 'sidrena_datum' ),
	array( 'usluge', 'legacy_fmcg', 'reference_day', '2025-05-02', true, 'Pogrešna grupa', 'reference_scope' ),
	array( 'proizvodi', 'legacy_fmcg', 'discount_previous_retail', '2025-05-02', true, 'Krivo pravilo', 'historical_method' ),
	array( 'usluge', 'general_goods_services', 'discount_lowest_30d', '2026-09-10', true, 'Krivo pravilo', 'historical_method' ),
	array( 'proizvodi', 'general_goods_services', 'reference_day', '2026-09-10', false, 'Dokaz', 'historical_verified' ),
	array( 'proizvodi', 'general_goods_services', 'reference_day', '2026-09-10', true, '', 'historical_source' ),
);
foreach ( $cases as $case ) {
	list( $kind, $scope, $method, $date, $verified, $source, $expected ) = $case;
	$input = array(
		'reference_scope' => $scope,
		'historical_method' => $method,
		'sidrena_datum' => $date,
		'historical_verified' => $verified,
		'historical_source' => $source,
	);
	$core_issue = \Brendigo\Bazna\Core::legalEvidenceIssue( $input, $kind );
	$wp_input = $input;
	$wp_input['historical_verified'] = $verified ? 'yes' : '';
	// Both engines must independently validate the source: do not massage the
	// verified flag or rewrite the returned issue to hide a parity failure.
	$wp_issue = Brendigo_Sidrena_Cijena_Official_Feed::evidence_issue( $wp_input, $kind );
	expect_official( $core_issue === $expected && $wp_issue === $expected,
		'WP and Core NN132 evidence parity: ' . implode( '/', array( $kind, $scope, $method, $date, $expected ) ) );
}
echo "PASS: NN 132/2026 official feed projection\n";
