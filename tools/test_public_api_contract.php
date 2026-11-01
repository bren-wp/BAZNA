<?php
/**
 * WordPress/Android public discovery contract test.
 *
 * A tiny WordPress stub verifies optional feed readiness without requiring a
 * real WP installation or leaking filesystem paths in public JSON.
 */

define( 'ABSPATH', __DIR__ . '/' );
define( 'BRENDIGO_SIDRENA_CIJENA_VERSION', '1.5.4' );

function sidrena_contract_assert( $condition, $label ) {
	if ( ! $condition ) {
		fwrite( STDERR, 'FAIL: ' . $label . PHP_EOL );
		exit( 1 );
	}
}
function add_action( $hook, $callback ) {
	return true;
}
function esc_url_raw( $url ) {
	return $url;
}
function rest_url( $route ) {
	return 'https://primjer.hr/wp-json/' . $route;
}
function rest_ensure_response( $data ) {
	return $data;
}
function add_query_arg( $key, $value, $url ) {
	$separator = false === strpos( $url, '?' ) ? '?' : '&';
	return $url . $separator . rawurlencode( $key ) . '=' . rawurlencode( $value );
}
class WP_REST_Server {
	const READABLE = 'GET';
}
class Brendigo_Sidrena_Cijena_Export {
	public static function instance() {
		return new self();
	}
	public function get_public_meta() {
		return array( 'generated' => 1790000000 );
	}
	public function get_cjenik_url() {
		return 'https://primjer.hr/cjenici';
	}
	public function get_archive_url() {
		return 'https://primjer.hr/arhiva';
	}
	public function get_public_url() {
		return 'https://primjer.hr/sidrena-cijena.xml';
	}
}
class Brendigo_Sidrena_Cijena_Legal {
	public static function public_contract() {
		return array( 'schema' => 1 );
	}
}
class Brendigo_Sidrena_Cijena_Settings {
	public static function instance() {
		return new self();
	}
	public function is_multi_poslovnica() {
		return true;
	}
}
class Brendigo_Sidrena_Cijena_Poslovnice {
	public static function instance() {
		return new self();
	}
	public function all() {
		return array( array( 'naziv' => 'Rijeka', 'oznaka' => 'ri-01' ) );
	}
}
class Brendigo_Sidrena_Cijena_Feed_Files {
	public static $dir;
	public static function instance() {
		return new self();
	}
	public function get_type_public_path( $kind, $format = 'csv', $oznaka = '' ) {
		$part = '' !== $oznaka ? '-' . $oznaka : '';
		return self::$dir . '/' . $kind . $part . '.' . $format;
	}
}

require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-public-api.php';

$dir = sys_get_temp_dir() . '/sidrena-api-contract-' . uniqid( '', true );
sidrena_contract_assert( mkdir( $dir, 0700 ), 'Test directory' );
Brendigo_Sidrena_Cijena_Feed_Files::$dir = $dir;

$products = $dir . '/proizvodi.csv';
$services = $dir . '/usluge.csv';
$outlet_products = $dir . '/proizvodi-ri-01.csv';

file_put_contents( $products, "naziv;maloprodajna_cijena\n" );
file_put_contents( $services, "naziv;maloprodajna_cijena\n" );
file_put_contents( $outlet_products, "naziv;maloprodajna_cijena\n" );

$api = Brendigo_Sidrena_Cijena_Public_API::instance();
$json = $api->index();
sidrena_contract_assert( 1 === $json['api_version'], 'Version remains v1' );
sidrena_contract_assert( true === $json['feeds']['proizvodi']['available_csv'], 'Product feed ready' );
sidrena_contract_assert( true === $json['feeds']['usluge']['available_csv'], 'Service feed ready' );
sidrena_contract_assert( true === $json['publication']['csv_ready'], 'Both main feeds published' );
sidrena_contract_assert( 'https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici/proizvodi/csv' === $json['feeds']['proizvodi']['csv'], 'Existing CSV URL unchanged' );
sidrena_contract_assert( true === $json['outlets'][0]['feeds']['proizvodi']['available_csv'], 'Outlet product available' );
sidrena_contract_assert( false === $json['outlets'][0]['feeds']['usluge']['available_csv'], 'Outlet service missing, not falsely ready' );
sidrena_contract_assert( false === strpos( json_encode( $json ), $dir ), 'No local filesystem paths in public JSON' );
sidrena_contract_assert( 1 === $json['legal']['schema'], 'Existing legal contract retained' );

unlink( $services );
$after = $api->index();
sidrena_contract_assert( false === $after['publication']['csv_ready'], 'Deleting published CSV correctly marks pending' );
sidrena_contract_assert( false === $after['feeds']['usluge']['available_csv'], 'Missing main CSV does not appear ready' );
sidrena_contract_assert( true === $after['feeds']['proizvodi']['available_csv'], 'Unaffected product CSV still available' );

unlink( $outlet_products );
unlink( $products );
rmdir( $dir );

fwrite( STDOUT, "PASS: SIDRENA Android/WordPress public API v1 contract\n" );
