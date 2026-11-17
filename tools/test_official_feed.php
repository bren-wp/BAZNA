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
);
$projected = Brendigo_Sidrena_Cijena_Official_Feed::project( $row, 'proizvodi' );
expect_official( array_keys( $projected ) === $goods, 'No extra compatibility fields in official export' );
expect_official( $projected['bazna_cijena'] === '3.90', 'Historical base is retained' );
expect_official( $projected['poseban_oblik_prodaje'] === 'da', 'Special sale flag' );
expect_official( $projected['ean_barkod'] === '0123456789012', 'Leading EAN zeros retained' );
expect_official( array() === Brendigo_Sidrena_Cijena_Official_Feed::missing_required_data( array( $row ), 'proizvodi' ), 'Complete row accepted' );
$incomplete = $row;
$incomplete['neto_kolicina'] = '';
expect_official( 1 === count( Brendigo_Sidrena_Cijena_Official_Feed::missing_required_data( array( $incomplete ), 'proizvodi' ) ), 'Net quantity checked when unit price is present' );
$incomplete['sidrena_cijena'] = '';
expect_official( 2 === count( Brendigo_Sidrena_Cijena_Official_Feed::missing_required_data( array( $incomplete ), 'proizvodi' ) ), 'Missing historic price cannot be inferred' );
expect_official( Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678903', 'oib' ), '11-digit OIB with correct check digit' );
expect_official( ! Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678901', 'oib' ), 'Incorrect OIB check digit rejected' );
expect_official( Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678', 'maticni_broj' ), '8-digit MB identifier shape' );
expect_official( ! Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678', 'oib' ), 'Wrong identity kind rejected' );
expect_official( ! Brendigo_Sidrena_Cijena_Official_Feed::valid_identifier( '12345678901/../', 'oib' ), 'Filename traversal rejected' );
echo "PASS: NN 132/2026 official feed projection\n";
