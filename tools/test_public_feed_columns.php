<?php
/** Standalone CSV/XML schema regression tests for SIDRENA Android. */
define( 'ABSPATH', __DIR__ . '/' );

require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-feed-files.php';

function sidrena_feed_expect( $ok, $reason ) {
	if ( ! $ok ) {
		fwrite( STDERR, "FAIL: " . $reason . PHP_EOL );
		exit( 1 );
	}
}

$files = Brendigo_Sidrena_Cijena_Feed_Files::instance();
$goods = $files->public_column_keys( array(), 'proizvodi' );
$services = $files->public_column_keys( array(), 'usluge' );

sidrena_feed_expect( in_array( 'slika_url', $goods, true ), 'Optional image URL exported for product feed' );
sidrena_feed_expect( ! in_array( 'slika_url', $services, true ), 'No invented image for services' );
sidrena_feed_expect( in_array( 'sidrena_cijena', $goods, true ), 'Anchor price remains in goods feed' );
sidrena_feed_expect( in_array( 'sidrena_datum', $goods, true ), 'Anchor reference date remains in goods feed' );
sidrena_feed_expect( in_array( 'maloprodajna_cijena', $goods, true ), 'Real current price remains' );
sidrena_feed_expect( in_array( 'dostupnost', $goods, true ), 'Real stock remains' );

$row = array(
	'id' => '7',
	'vrsta' => 'proizvod',
	'naziv' => 'Maslinovo ulje 1 L',
	'sku' => 'UL-7',
	'slika_url' => 'https://primjer.hr/wp-content/uploads/ulje.webp',
	'maloprodajna_cijena' => '5.99',
	'sidrena_cijena' => '8.49',
	'sidrena_datum' => '2026-10-10',
	'dostupnost' => 'dostupno',
);
$csv = $files->build_csv_string( array( $row ), 'proizvodi' );
sidrena_feed_expect( 0 === strncmp( $csv, "\xEF\xBB\xBF", 3 ), 'UTF-8 BOM retained' );
sidrena_feed_expect( false !== strpos( $csv, 'slika_url' ), 'Image header present' );
sidrena_feed_expect( false !== strpos( $csv, 'ulje.webp' ), 'Genuine image URL exported' );
sidrena_feed_expect( false !== strpos( $csv, '5,99' ), 'Croatian current price format' );
sidrena_feed_expect( false !== strpos( $csv, '8,49' ), 'Croatian anchor price format' );

$service = $files->build_csv_string(
	array(
		array(
			'id' => 's1',
			'vrsta' => 'usluga',
			'naziv' => 'Montaža',
			'maloprodajna_cijena' => '30.00',
		),
	),
	'usluge'
);
sidrena_feed_expect( false === strpos( $service, 'slika_url' ), 'Service CSV unchanged' );
sidrena_feed_expect( false !== strpos( $service, '30,00' ), 'Service current price format' );

fwrite( STDOUT, "PASS: SIDRENA optional product images + CSV compatibility\n" );
