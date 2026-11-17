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
echo "PASS: NN 132/2026 official feed projection\n";
