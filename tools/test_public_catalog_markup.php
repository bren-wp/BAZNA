<?php
/**
 * Standalone test of the WordPress public catalog HTML contract that Android
 * screen 05 mirrors visually. No WordPress installation/database is required.
 */
define( 'ABSPATH', __DIR__ . '/' );

function sidrena_markup_expect( $condition, $label ) {
	if ( ! $condition ) {
		fwrite( STDERR, 'FAIL: ' . $label . PHP_EOL );
		exit( 1 );
	}
}
function esc_attr( $value ) {
	return htmlspecialchars( (string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8' );
}
function esc_html( $value ) {
	return esc_attr( $value );
}
function esc_url( $value ) {
	return esc_attr( $value );
}
function esc_html__( $message, $text_domain ) {
	return $message;
}
function __( $message, $text_domain ) {
	return $message;
}
function esc_attr__( $message, $text_domain ) {
	return $message;
}
class Brendigo_Sidrena_Cijena_Catalog {
	public static function instance() {
		return new self();
	}
	public function format_amount( $amount ) {
		return '' !== trim( (string) $amount ) ? (string) $amount . ' €' : '—';
	}
}
class Brendigo_Sidrena_Cijena_Upit {
	public static function instance() {
		return new self();
	}
	public function for_oznaka( $outlet ) {
		return array();
	}
}

require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-feed-files.php';

$rows = array(
	array(
		'id' => 's1',
		'vrsta' => 'usluga',
		'naziv' => 'Servis klima-uređaja',
		'url' => 'https://primjer.hr/servis',
		'maloprodajna_cijena' => '45,00',
		'sidrena_cijena' => '60,00',
		'sidrena_datum' => '2026-10-09',
	),
	array(
		'id' => 'p2',
		'vrsta' => 'proizvod',
		'naziv' => '<script>alert(1)</script>',
		'maloprodajna_cijena' => '10,00',
		'sidrena_cijena' => '12,00',
		'sidrena_datum' => '2026-10-09',
	),
);
$html = Brendigo_Sidrena_Cijena_Feed_Files::instance()->build_cjenik_embed_html( $rows, 'public' );
sidrena_markup_expect( false !== strpos( $html, 'data-kind="usluga"' ), 'Public service row is identified' );
sidrena_markup_expect( false !== strpos( $html, 'data-kind="proizvod"' ), 'Public product row is identified' );
sidrena_markup_expect( false !== strpos( $html, 'data-field="maloprodajna_cijena"' ), 'Price key is stable' );
sidrena_markup_expect( false !== strpos( $html, 'data-field="sidrena_cijena"' ), 'Reference key is stable' );
sidrena_markup_expect( false !== strpos( $html, 'data-field="sidrena_datum"' ), 'Reference date key stays visible' );
sidrena_markup_expect( false !== strpos( $html, '45,00 €' ), 'Actual service price retained' );
sidrena_markup_expect( false !== strpos( $html, '60,00 €' ), 'Historical reference value retained' );
sidrena_markup_expect( false === strpos( $html, '<script>' ), 'Product names are escaped in public HTML' );
sidrena_markup_expect( false !== strpos( $html, '&lt;script&gt;' ), 'Escaped name is still visible' );
sidrena_markup_expect( false !== strpos( $html, 'data-label="Sidrena cijena"' ), 'Translated price cell label retained' );
sidrena_markup_expect( false !== strpos( $html, 'href="https://primjer.hr/servis"' ), 'Existing public service link retained' );

if ( isset( $argv ) && in_array( '--fixture', $argv, true ) ) {
    // Export the actual public HTML builder output for headless browser QA.
    // This is a test-only document; never exposed on the production site.
    $css_file = realpath( __DIR__ . '/../wordpress/brendigo-sidrena-cijena/assets/css/cjenik.css' );
    if ( ! $css_file ) {
        fwrite( STDERR, "Missing public price-list CSS fixture\n" );
        exit( 1 );
    }
    $css_url = 'file://' . $css_file;
    echo '<!doctype html><html lang="hr"><head><meta charset="utf-8">';
    echo '<meta name="viewport" content="width=device-width, initial-scale=1">';
    echo '<link rel="stylesheet" href="' . esc_attr( $css_url ) . '">';
    echo '</head><body><main><section class="cjenik-hero"><p class="cjenik-kicker">SIDRENA</p>';
    echo '<h1>Javni cjenici</h1><p>Objavljeni proizvodi i usluge</p></section>';
    echo $html;
    echo '</main></body></html>';
    exit( 0 );
}

fwrite( STDOUT, "PASS: SIDRENA public service/product HTML, dates and safe mobile card fields\n" );
