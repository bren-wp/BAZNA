<?php
/**
 * WordPress/Android public discovery contract test.
 *
 * A tiny WordPress stub verifies optional feed readiness without requiring a
 * real WP installation or leaking filesystem paths in public JSON.
 */

define( 'ABSPATH', __DIR__ . '/' );
define( 'BRENDIGO_SIDRENA_CIJENA_VERSION', '1.5.10' );

function sidrena_contract_assert( $condition, $label ) {
	if ( ! $condition ) {
		fwrite( STDERR, 'FAIL: ' . $label . PHP_EOL );
		exit( 1 );
	}
}
function add_action( $hook, $callback ) {
	return true;
}
function register_rest_route( $namespace, $route, $args ) {
	global $sidrena_registered_routes;
	$sidrena_registered_routes[ $namespace . $route ] = $args;
}
function esc_url_raw( $url ) {
	return $url;
}
function rest_url( $route ) {
	global $sidrena_plain_permalinks;
	if ( ! empty( $sidrena_plain_permalinks ) ) {
		return 'https://primjer.hr/?rest_route=/' . $route;
	}
	return 'https://primjer.hr/wp-json/' . $route;
}
// Mirror the WordPress REST response API, including HTTP headers, while
// retaining array access for the existing contract assertions.
class WP_REST_Response implements ArrayAccess, JsonSerializable {
    private $data;
    private $headers = array();
    public function __construct( $data = null, $status = 200 ) { $this->data = $data; }
    public function header( $key, $value ) { $this->headers[ $key ] = $value; }
    public function get_headers() { return $this->headers; }
    #[\ReturnTypeWillChange]
    public function jsonSerialize() { return $this->data; }
    public function offsetExists( $offset ): bool { return isset( $this->data[ $offset ] ); }
    #[\ReturnTypeWillChange]
    public function offsetGet( $offset ) { return $this->data[ $offset ]; }
    public function offsetSet( $offset, $value ): void { $this->data[ $offset ] = $value; }
    public function offsetUnset( $offset ): void { unset( $this->data[ $offset ] ); }
}
function rest_ensure_response( $data ) {
	return new WP_REST_Response( $data );
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
$api->register();
sidrena_contract_assert( isset( $sidrena_registered_routes['brendigo-sidrena/v1/cjenici'] ), 'Discovery route registered on plugin init' );
sidrena_contract_assert( isset( $sidrena_registered_routes['brendigo-sidrena/v1/arhiva'] ), 'Archive route registered on plugin init' );
sidrena_contract_assert( isset( $sidrena_registered_routes['brendigo-sidrena/v1/cjenici/(?P<kind>proizvodi|usluge)/(?P<format>xml|csv)'] ), 'CSV/XML feed route registered' );
foreach ( $sidrena_registered_routes as $registered ) {
	sidrena_contract_assert( WP_REST_Server::READABLE === $registered['methods'], 'Public endpoints must be read-only' );
	sidrena_contract_assert( '__return_true' === $registered['permission_callback'], 'Public endpoints have no admin authentication' );
}
$json = $api->index();
sidrena_contract_assert(
    'no-store, max-age=0, must-revalidate' === $json->get_headers()['Cache-Control'],
    'Public discovery must not cache publication/readiness flags'
);
sidrena_contract_assert( 1 === $json['api_version'], 'Version remains v1' );
sidrena_contract_assert( 'public_read_only' === $json['connection']['mode'], 'Mobile connection must never require admin account' );
sidrena_contract_assert( false === $json['connection']['authentication_required'], 'No credentials on public price API' );
sidrena_contract_assert( true === $json['connection']['csv_publication_required'], 'CSV readiness is not fabricated on first install' );
sidrena_contract_assert( 'https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici' === $json['connection']['rest_endpoint'], 'Canonical REST connection endpoint present' );
sidrena_contract_assert( true === $json['feeds']['proizvodi']['available_csv'], 'Product feed ready' );
sidrena_contract_assert( true === $json['feeds']['usluge']['available_csv'], 'Service feed ready' );
sidrena_contract_assert( true === $json['publication']['csv_ready'], 'Both main feeds published' );
sidrena_contract_assert( false === $json['publication']['pending_changes'], 'No unverified changes on a clean standalone installation' );
sidrena_contract_assert( false === $json['publication']['compliance_certified'], 'Published CSV does not certify legally correct prices' );
sidrena_contract_assert( 'https://primjer.hr/wp-json/brendigo-sidrena/v1/cjenici/proizvodi/csv' === $json['feeds']['proizvodi']['csv'], 'Existing CSV URL unchanged' );
sidrena_contract_assert( true === $json['outlets'][0]['feeds']['proizvodi']['available_csv'], 'Outlet product available' );
sidrena_contract_assert( false === $json['outlets'][0]['feeds']['usluge']['available_csv'], 'Outlet service missing, not falsely ready' );
sidrena_contract_assert( false === strpos( json_encode( $json ), $dir ), 'No local filesystem paths in public JSON' );
sidrena_contract_assert( 1 === $json['legal']['schema'], 'Existing legal contract retained' );

// A real WordPress installation with plain permalinks returns query-style
// rest_url() values. Verify the plugin publishes the SAME contract and keeps
// outlet query parameters without requiring admin authentication.
$sidrena_plain_permalinks = true;
$plain = $api->index();
sidrena_contract_assert(
	'https://primjer.hr/?rest_route=/brendigo-sidrena/v1/cjenici' === $plain['connection']['rest_endpoint'],
	'Plain permalink connection URL is discoverable'
);
sidrena_contract_assert(
	'https://primjer.hr/?rest_route=/brendigo-sidrena/v1/cjenici/proizvodi/csv' === $plain['feeds']['proizvodi']['csv'],
	'Plain permalink product CSV endpoint'
);
sidrena_contract_assert(
	false !== strpos( $plain['outlets'][0]['feeds']['proizvodi']['csv'], '&oznaka=ri-01' ),
	'Plain permalink outlet parameters preserved'
);
$sidrena_plain_permalinks = false;

unlink( $services );
$after = $api->index();
sidrena_contract_assert( false === $after['publication']['csv_ready'], 'Deleting published CSV correctly marks pending' );
sidrena_contract_assert( false === $after['feeds']['usluge']['available_csv'], 'Missing main CSV does not appear ready' );
sidrena_contract_assert( true === $after['feeds']['proizvodi']['available_csv'], 'Unaffected product CSV still available' );

unlink( $outlet_products );
unlink( $products );
rmdir( $dir );

fwrite( STDOUT, "PASS: SIDRENA Android/WordPress public API v1 contract\n" );
