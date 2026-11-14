<?php
/**
 * WordPress/Android public discovery contract test.
 *
 * A tiny WordPress stub verifies optional feed readiness without requiring a
 * real WP installation or leaking filesystem paths in public JSON.
 */

define( 'ABSPATH', __DIR__ . '/' );
define( 'BRENDIGO_SIDRENA_CIJENA_VERSION', '1.5.19' );

function sidrena_contract_assert( $condition, $label ) {
	if ( ! $condition ) {
		fwrite( STDERR, 'FAIL: ' . $label . PHP_EOL );
		exit( 1 );
	}
}
$sidrena_test_options = array();
function get_option( $key, $fallback = false ) {
	global $sidrena_test_options;
	return array_key_exists( $key, $sidrena_test_options ) ? $sidrena_test_options[ $key ] : $fallback;
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
function sanitize_text_field( $value ) {
    return trim( strip_tags( (string) $value ) );
}
// Equivalent to WordPress JSON encoding in this isolated PHP test fixture.
function wp_json_encode( $value ) {
	return json_encode( $value, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE );
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
class Brendigo_Sidrena_Cijena_License {
    public static function valid() { return true; }
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
    public function normalize_oznaka( $code ) { return trim( (string) $code ); }
}
class Brendigo_Sidrena_Cijena_Archive {
    public static function instance() { return new self(); }
    public function separate_archive_files( $kind, $outlet ) { return array(); }
}
class SIDRENA_Test_Request {
    private $params;
    public function __construct( $params ) { $this->params = $params; }
    public function get_param( $name ) {
        return isset( $this->params[ $name ] ) ? $this->params[ $name ] : null;
    }
}
class Brendigo_Sidrena_Cijena_Poslovnice {
	public static function instance() {
		return new self();
	}
	public function all() {
		return array( array( 'naziv' => 'Rijeka', 'oznaka' => 'ri-01', 'adresa' => 'Korzo 12, Rijeka' ) );
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
    public function get_type_public_url( $kind, $format = 'csv', $oznaka = '' ) {
        return 'https://primjer.hr/wp-content/uploads/sidrena-cijena/' . $kind . '.' . $format;
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
sidrena_contract_assert( isset( $sidrena_registered_routes['brendigo-bazna/v1/cjenici'] ), 'Canonical BAZNA discovery route registered' );
sidrena_contract_assert( isset( $sidrena_registered_routes['brendigo-bazna/v1/arhiva'] ), 'Canonical BAZNA archive registered' );
sidrena_contract_assert( isset( $sidrena_registered_routes['brendigo-bazna/v1/usluge-na-upit'] ), 'Canonical public inquiry route registered' );
sidrena_contract_assert( isset( $sidrena_registered_routes['brendigo-bazna/v1/cjenici/(?P<kind>proizvodi|usluge)/(?P<format>xml|csv)'] ), 'Canonical BAZNA CSV/XML feeds registered' );
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
$archive_response = $api->archive_index( new SIDRENA_Test_Request( array() ) );
sidrena_contract_assert(
    'no-store, max-age=0, must-revalidate' === $archive_response->get_headers()['Cache-Control'],
    'Public archive must not retain stale publication information'
);
$feed_response = $api->feed( new SIDRENA_Test_Request( array(
    'kind' => 'proizvodi', 'format' => 'csv', 'oznaka' => '',
) ) );
sidrena_contract_assert(
    'no-store, max-age=0, must-revalidate' === $feed_response->get_headers()['Cache-Control'],
    'Public CSV redirect must be revalidated after republishing'
);
sidrena_contract_assert(
    'https://primjer.hr/wp-content/uploads/sidrena-cijena/proizvodi.csv' ===
        $feed_response->get_headers()['Location'],
    'Public feed redirect remains same-origin'
);
sidrena_contract_assert( 1 === $json['api_version'], 'Version remains v1' );
sidrena_contract_assert( 'wordpress' === $json['platform'], 'Publisher type is accurate' );
sidrena_contract_assert( 'BAZNA' === $json['branding']['name'], 'New product name in public Android discovery' );
sidrena_contract_assert( 'Bazne cijene' === $json['branding']['description'], 'Human-readable brand description' );
sidrena_contract_assert( true === $json['licensing']['required'], 'WordPress license controls are declared' );
sidrena_contract_assert( true === $json['licensing']['active'], 'Activation is represented without key disclosure' );
sidrena_contract_assert( false === strpos( wp_json_encode( $json ), 'BAZNA-00001-' ), 'No license keys in public REST response' );
sidrena_contract_assert( 'public_read_only' === $json['connection']['mode'], 'Mobile connection must never require admin account' );
sidrena_contract_assert( false === $json['connection']['authentication_required'], 'No credentials on public price API' );
sidrena_contract_assert( true === $json['connection']['csv_publication_required'], 'CSV readiness is not fabricated on first install' );
sidrena_contract_assert( 'https://primjer.hr/wp-json/brendigo-bazna/v1/cjenici' === $json['connection']['rest_endpoint'], 'Canonical REST connection endpoint present' );
sidrena_contract_assert( 'https://primjer.hr/wp-json/brendigo-bazna/v1/usluge-na-upit' === $json['on_request'], 'Read-only published inquiry URL exposed' );
$not_published = $api->on_request_services();
sidrena_contract_assert( array() === $not_published['items'], 'Unpublished on-request services do not leak' );
$sidrena_test_options['sidrena_cijena_upit'] = array( array( 'id' => '11', 'naziv' => 'Privatni nacrt', 'kriteriji' => 'Ne objavljuj' ) );
sidrena_contract_assert( array() === $api->on_request_services()['items'], 'Draft option is never a public source' );
$sidrena_test_options['brendigo_bazna_published_inquiry_v1'] = array(
    array( 'id' => '1', 'name' => 'Montaža', 'criteria' => 'Ovisno o opsegu', 'outlet' => 'ri-01' ),
    array( 'id' => '2', 'name' => '', 'criteria' => 'Nevaljan redak', 'outlet' => '' ),
);
$inquiry = $api->on_request_services();
sidrena_contract_assert( 1 === $inquiry['schema'] && 1 === count( $inquiry['items'] ), 'Only sanitized published inquiries returned' );
sidrena_contract_assert( 'Ovisno o opsegu' === $inquiry['items'][0]['criteria'], 'Publication criteria preserved' );
sidrena_contract_assert( 'no-store, max-age=0, must-revalidate' === $inquiry->get_headers()['Cache-Control'], 'Do not cache publication snapshots' );
$validPublicRequest = new class {
    public function get_param( $key ) {
        return array( 'outlet' => 'ri-01', 'q' => 'OPSEGU' )[ $key ] ?? null;
    }
};
sidrena_contract_assert( 1 === count( $api->on_request_services( $validPublicRequest )['items'] ),
    'REST outlet and multi-word query operate on published snapshots only' );
$unknownPublicRequest = new class {
    public function get_param( $key ) {
        return array( 'outlet' => 'no-other-branch', 'q' => 'OPSEGU' )[ $key ] ?? null;
    }
};
sidrena_contract_assert( array() === $api->on_request_services( $unknownPublicRequest )['items'],
    'REST public inquiry does not mix branch results' );
$badPublicRequest = new class {
    public function get_param( $key ) {
        return 'q' === $key ? array( 'x' ) : '';
    }
};
sidrena_contract_assert( array() === $api->on_request_services( $badPublicRequest )['items'],
    'REST inquiry rejects array-valued search pollution' );

sidrena_contract_assert( true === $json['feeds']['proizvodi']['available_csv'], 'Product feed ready' );
sidrena_contract_assert( true === $json['feeds']['usluge']['available_csv'], 'Service feed ready' );
sidrena_contract_assert( true === $json['publication']['csv_ready'], 'Both main feeds published' );
sidrena_contract_assert( false === $json['publication']['csv_ready_all_outlets'], 'Unpublished outlet cannot borrow webshop readiness' );
sidrena_contract_assert( false === $json['publication']['pending_changes'], 'No unverified changes on a clean standalone installation' );
sidrena_contract_assert( false === $json['publication']['compliance_certified'], 'Published CSV does not certify legally correct prices' );
sidrena_contract_assert( 'https://primjer.hr/wp-json/brendigo-bazna/v1/cjenici/proizvodi/csv' === $json['feeds']['proizvodi']['csv'], 'Existing CSV URL unchanged' );
sidrena_contract_assert( 'Korzo 12, Rijeka' === $json['outlets'][0]['address'], 'Only the configured public business address is published' );
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
	'https://primjer.hr/?rest_route=/brendigo-bazna/v1/cjenici' === $plain['connection']['rest_endpoint'],
	'Plain permalink connection URL is discoverable'
);
sidrena_contract_assert(
	'https://primjer.hr/?rest_route=/brendigo-bazna/v1/cjenici/proizvodi/csv' === $plain['feeds']['proizvodi']['csv'],
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
sidrena_contract_assert( false === $after['publication']['csv_ready_all_outlets'], 'All-outlet status follows missing main CSV' );
sidrena_contract_assert( false === $after['feeds']['usluge']['available_csv'], 'Missing main CSV does not appear ready' );
sidrena_contract_assert( true === $after['feeds']['proizvodi']['available_csv'], 'Unaffected product CSV still available' );

unlink( $outlet_products );
unlink( $products );
rmdir( $dir );

fwrite( STDOUT, "PASS: BAZNA Android/WordPress public API v1 contract\n" );
