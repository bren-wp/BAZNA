<?php
/** Security, conflict and privacy contract for authenticated WP draft sync. */
declare(strict_types=1);
define( 'ABSPATH', __DIR__ . '/' );
$GLOBALS['sync_rows'] = array();
$GLOBALS['sync_lock'] = '';
$GLOBALS['sync_ssl'] = true;
$GLOBALS['sync_manager'] = true;
$GLOBALS['sync_woo'] = false;
$GLOBALS['sync_routes'] = array();

function sync_check( $ok, $message ) {
    if ( ! $ok ) { fwrite( STDERR, "FAIL: $message\n" ); exit( 1 ); }
}
class WP_Error {
    public $code;
    public $data;
    public function __construct( $code, $message = '', $data = array() ) {
        $this->code = $code; $this->data = $data;
    }
    public function get_error_code() { return $this->code; }
}
class WP_REST_Server {
    const READABLE = 'GET';
    const CREATABLE = 'POST';
}
class SyncResponse {
    public $data;
    public $headers = array();
    public function __construct( $data ) { $this->data = $data; }
    public function header( $key, $value ) { $this->headers[$key] = $value; }
    public function get_data() { return $this->data; }
}
class SyncRequest {
    private $body;
    public function __construct( $data ) { $this->body = json_encode( $data ); }
    public function get_header( $key ) { return strlen( $this->body ); }
    public function get_body() { return $this->body; }
}
class Brendigo_Sidrena_Cijena_Plugin {
    public static function current_user_can_manage() { return $GLOBALS['sync_manager']; }
    public static function woocommerce_active() { return $GLOBALS['sync_woo']; }
}
class Brendigo_Sidrena_Cijena_Automation {
    public static $pending = array();
    public static function instance() { return new self(); }
    public function mark_pending( $reason ) { self::$pending[] = $reason; }
}
class Brendigo_Sidrena_Cijena_Catalog {
    const OPTION_KEY = 'sidrena_cijena_catalog';
    public static function instance() { return new self(); }
    public function all() { return $GLOBALS['sync_rows']; }
    public function refresh_for_sync() {}
    public function save( $rows ) {
        $before = $GLOBALS['sync_rows'];
        foreach ( $rows as $index => $row ) {
            if ( empty( $row['id'] ) ) { $rows[$index]['id'] = (string) ( $index + 1 ); }
            foreach ( $before as $old ) {
                if ( (string) $old['id'] === (string) $rows[$index]['id'] ) {
                    foreach ( array( 'sidrena_cijena', 'sidrena_datum', 'historical_source',
                            'reference_scope', 'historical_method' ) as $key ) {
                        if ( (string) ($old[$key] ?? '') !== (string) ($row[$key] ?? '') ) {
                            $rows[$index]['historical_verified'] = '';
                            break;
                        }
                    }
                }
            }
        }
        $GLOBALS['sync_rows'] = $rows;
        return $rows;
    }
}
class SyncDb {
    public $options = 'wp_options';
    public function prepare( $sql, ...$args ) { return json_encode( $args ); }
    public function query( $encoded ) {
        $args = json_decode( $encoded, true );
        if ( is_array( $args ) && $args[0] === Brendigo_Sidrena_Cijena_Sync_Draft::LOCK_KEY &&
            $args[1] === $GLOBALS['sync_lock'] ) {
            $GLOBALS['sync_lock'] = '';
            return 1;
        }
        return 0;
    }
}
$GLOBALS['wpdb'] = new SyncDb();
function is_ssl() { return $GLOBALS['sync_ssl']; }
function wp_json_encode( $data, $options = 0 ) { return json_encode( $data, $options ); }
function rest_ensure_response( $data ) { return new SyncResponse( $data ); }
function register_rest_route( $ns, $route, $args ) { $GLOBALS['sync_routes'][$ns . $route] = $args; }
function wp_cache_delete( $key, $group ) {}
function get_option( $key, $fallback = '' ) {
    return Brendigo_Sidrena_Cijena_Sync_Draft::LOCK_KEY === $key ? $GLOBALS['sync_lock'] : $fallback;
}
function add_option( $key, $value, $deprecated = '', $autoload = false ) {
    if ( $GLOBALS['sync_lock'] !== '' ) { return false; }
    $GLOBALS['sync_lock'] = $value;
    return true;
}
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-sync-draft.php';

$sync = 'Brendigo_Sidrena_Cijena_Sync_Draft';
$sync::register();
sync_check( isset( $GLOBALS['sync_routes']['brendigo-bazna/v1/sync/draft'] ), 'Only private sync route is registered' );
$routes = $GLOBALS['sync_routes']['brendigo-bazna/v1/sync/draft'];
sync_check( $routes[0]['permission_callback'] === array( $sync, 'authorize' ) &&
    $routes[1]['permission_callback'] === array( $sync, 'authorize' ), 'GET and POST both authorize' );
$GLOBALS['sync_ssl'] = false;
sync_check( $sync::authorize() instanceof WP_Error, 'Rejects HTTP even with logged-in user' );
$GLOBALS['sync_ssl'] = true;
$GLOBALS['sync_manager'] = false;
sync_check( $sync::authorize() instanceof WP_Error, 'Rejects unprivileged user' );
$GLOBALS['sync_manager'] = true;
$GLOBALS['sync_woo'] = true;
sync_check( $sync::authorize() instanceof WP_Error, 'WooCommerce source-of-truth not overwritten' );
$GLOBALS['sync_woo'] = false;
sync_check( true === $sync::authorize(), 'Authenticated HTTPS administrator allowed' );

$read = $sync::read();
sync_check( $read instanceof SyncResponse && $read->data['schema'] === 1 &&
    $read->data['mode'] === 'standalone_draft' && $read->data['rows'] === array(), 'Read current private draft' );
sync_check( false !== strpos( $read->headers['Cache-Control'], 'no-store' ), 'Never cache sensitive draft or evidence' );
$base = $read->data['revision'];
sync_check( hash('sha256', '[]') === $base, 'Deterministic initial revision' );
$create = array( 'expected_revision' => $base, 'operation' => 'upsert', 'id' => '',
    'row' => array( 'naziv' => 'Primjer', 'cijena_redovna' => '7,00',
        'sidrena_cijena' => '8,00', 'historical_verified' => 'yes' ) );
$invalid = $sync::write( new SyncRequest( $create ) );
sync_check( $invalid instanceof WP_Error && $invalid->code === 'bazna_sync_fields',
    'Clients cannot remotely set historical_verified' );
unset( $create['row']['historical_verified'] );
$created = $sync::write( new SyncRequest( $create ) );
sync_check( $created instanceof SyncResponse && $created->data['published'] === false &&
    count( $GLOBALS['sync_rows'] ) === 1 && $GLOBALS['sync_rows'][0]['historical_verified'] === '',
    'Creates private draft without documentary attestation or publication' );
sync_check( '' === $GLOBALS['sync_lock'], 'Atomic write releases lock after success' );
sync_check( Brendigo_Sidrena_Cijena_Automation::$pending === array( 'sync-draft' ), 'Remote draft edit is pending and never queued for public auto-release' );

$conflict = $sync::write( new SyncRequest( $create ) );
sync_check( $conflict instanceof WP_Error && $conflict->code === 'bazna_sync_conflict' &&
    $conflict->data['status'] === 409 && count( $GLOBALS['sync_rows'] ) === 1,
    'Stale version cannot overwrite newer draft' );
$revision = $created->data['revision'];
$edit = array( 'expected_revision' => $revision, 'operation' => 'upsert', 'id' => '1',
    'row' => array( 'naziv' => 'Izmijenjeni primjer', 'cijena_redovna' => '9,00' ) );
$changed = $sync::write( new SyncRequest( $edit ) );
sync_check( $changed instanceof SyncResponse && $GLOBALS['sync_rows'][0]['naziv'] === 'Izmijenjeni primjer' &&
    $GLOBALS['sync_rows'][0]['sidrena_cijena'] === '8,00', 'Patch retains unmentioned historical values' );
$GLOBALS['sync_lock'] = wp_json_encode( array( 'token' => 'other', 'until' => time() + 100 ) );
$locked = $sync::write( new SyncRequest( array_merge( $edit, array( 'expected_revision' => $changed->data['revision'] ) ) ) );
sync_check( $locked instanceof WP_Error && $locked->code === 'bazna_sync_locked', 'Concurrent writer is not overwritten' );
$GLOBALS['sync_lock'] = '';
$delete = array( 'expected_revision' => $changed->data['revision'], 'operation' => 'delete', 'id' => '1' );
sync_check( $sync::write( new SyncRequest( $delete ) ) instanceof WP_Error, 'Deletion requires confirmation' );
$delete['confirm_delete'] = true;
$deleted = $sync::write( new SyncRequest( $delete ) );
sync_check( $deleted instanceof SyncResponse && $GLOBALS['sync_rows'] === array(), 'Explicit delete updates only draft' );
echo "PASS: BAZNA authenticated sync, revision conflicts, lock, evidence and no publication\n";
