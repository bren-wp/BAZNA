<?php
/** Standalone regression: a current price never becomes an invented historical fact. */
define( 'ABSPATH', __DIR__ . '/' );
define( 'BRENDIGO_SIDRENA_CIJENA_META_DATE', '_brendigo_sidrena_datum' );

function legal_safety_assert( $value, $label ) {
	if ( ! $value ) {
		fwrite( STDERR, 'FAIL: ' . $label . PHP_EOL );
		exit( 1 );
	}
}
function add_action( $hook, $callback, $priority = 10, $args = 1 ) {
	return true;
}
function wp_unslash( $text ) {
	return $text;
}
function sanitize_text_field( $text ) {
	return trim( (string) $text );
}
function get_post_meta( $id, $key, $single ) {
	global $sidrena_dates;
	return isset( $sidrena_dates[ $id ] ) ? $sidrena_dates[ $id ] : '';
}
function delete_option( $key ) {
	return true;
}
function wp_clear_scheduled_hook( $hook ) {
	return true;
}
class WC_Product {
	public function get_id() { return 10; }
	public function get_regular_price() { return '99.00'; }
}
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/trait-row-helpers.php';
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-money.php';
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-catalog.php';
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-product-meta.php';
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-activator.php';

$sidrena_dates = array();
$meta = Brendigo_Sidrena_Cijena_Product_Meta::instance();
$manual = Brendigo_Sidrena_Cijena_Catalog::instance();

legal_safety_assert( '' === $meta->get_display_date( 10 ), 'No fabricated default for WooCommerce item' );
legal_safety_assert( '' === $manual->display_date( array( 'sidrena_datum' => '' ) ), 'No fabricated date for manual service/product' );
legal_safety_assert( false === $meta->snapshot_if_empty( new WC_Product(), '02.11.2026.' ), 'Today regular price is never a sidrena reference' );

$sidrena_dates[10] = '02.05.2025.';
legal_safety_assert( '02.05.2025.' === $meta->get_display_date( 10 ), 'Existing FMCG reference date remains unchanged' );
legal_safety_assert(
	'10.09.2026.' === $manual->display_date( array( 'sidrena_datum' => '10.09.2026.' ) ),
	'Explicit general reference date remains unchanged'
);
legal_safety_assert( '' === $manual->display_date( array( 'sidrena_datum' => '02.11.2026-invalid' ) ), 'Malformed dates are not accepted' );

$legacy = Brendigo_Sidrena_Cijena_Activator::process_batch( 1, true );
legal_safety_assert( ! empty( $legacy['blocked'] ) && 0 === $legacy['written'] && true === $legacy['done'], 'Queued legacy mass snapshots are blocked without changing stored values' );

fwrite( STDOUT, "PASS: SIDRENA historic-price evidence, reference dates and legacy snapshot safety\n" );
