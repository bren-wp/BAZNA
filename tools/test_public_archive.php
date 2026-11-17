<?php
/**
 * BAZNA NN 132/2026 public official archive discovery regression.
 * Uses a temporary disk fixture, never merchants' private catalog data.
 */
declare(strict_types=1);
define( 'ABSPATH', __DIR__ . '/' );
$GLOBALS['bazna_archive_test_dir'] = sys_get_temp_dir() . '/bazna-official-archive-' . bin2hex( random_bytes( 6 ) );
mkdir( $GLOBALS['bazna_archive_test_dir'], 0700, true );
function bazna_archive_assert( bool $ok, string $label ): void {
    if ( ! $ok ) { fwrite( STDERR, "FAIL: $label\n" ); exit( 1 ); }
}
function trailingslashit( $path ) { return rtrim( $path, '/\\' ) . '/'; }
function wp_timezone() { return new DateTimeZone( 'Europe/Zagreb' ); }
function wp_date( $format, $stamp = null ) { return ( new DateTimeImmutable( '@' . ( null === $stamp ? time() : $stamp ) ) )->setTimezone( wp_timezone() )->format( $format ); }
function esc_url_raw( $url ) { return $url; }
function esc_url( $url ) { return htmlspecialchars( $url, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8' ); }
function esc_html( $text ) { return htmlspecialchars( (string) $text, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8' ); }
function esc_attr( $text ) { return esc_html( $text ); }
function esc_html__( $s, $domain ) { return esc_html( $s ); }
function wp_upload_dir( ...$args ) { return array( 'baseurl' => 'https://example.test/wp-content/uploads', 'error' => false ); }
class Brendigo_Sidrena_Cijena_Settings {
    public static function instance() { return new self(); }
    public function get( $key, $default = '' ) {
        return 'trader_identifier' === $key ? '12345678903' : ( 'trader_identifier_kind' === $key ? 'oib' : $default );
    }
    public function normalize_oznaka( $code ) { return preg_replace( '/[^a-zA-Z0-9-]/', '', (string) $code ); }
}
class Brendigo_Sidrena_Cijena_Official_Feed {
    public static function valid_identifier( $id, $kind ) { return 'oib' === $kind && '12345678903' === $id; }
}
class Brendigo_Sidrena_Cijena_Feed_Files {
    public static function instance() { return new self(); }
    public function get_public_dir() { return $GLOBALS['bazna_archive_test_dir']; }
}
class Brendigo_Sidrena_Cijena_Filesystem {
    public static function dirlist( $dir ) {
        if ( ! is_dir( $dir ) ) { return array(); }
        $out = array();
        foreach ( scandir( $dir ) as $name ) {
            if ( '.' === $name || '..' === $name ) { continue; }
            $out[ $name ] = array( 'type' => is_file( $dir . '/' . $name ) ? 'f' : 'd', 'size' => filesize( $dir . '/' . $name ) );
        }
        return $out;
    }
    public static function is_dir( $path ) { return is_dir( $path ); }
}
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-archive.php';
$names = array(
    '12345678903_webshop_Ulica 1_001_ROBA_1_20261117_0930.csv',
    '12345678903_webshop_Ulica 1_001_ROBA_2_20261117_1015.csv',
    '12345678903_webshop_Ulica 1_001_USLUGE_3_20261117_1020.xml',
    '12345678903_prodavaonica_Korzo 10_RI-01_ROBA_4_20261117_1045.csv',
    '12345678901_webshop_Ulica 1_001_ROBA_9_20261117_1050.csv',
    '12345678903_webshop_Ulica 1_001_ROBA_6_20261117_2588.csv',
);
foreach ( $names as $name ) { file_put_contents( $GLOBALS['bazna_archive_test_dir'] . '/' . $name, 'public immutable fixture' ); }
$archive = Brendigo_Sidrena_Cijena_Archive::instance();
bazna_archive_assert( '' === $archive->sanitize_archive_name( '../private.csv' ), 'Raw traversal rejected before filesystem lookup' );
bazna_archive_assert( '' === $archive->sanitize_archive_name( '%2e%2e%2fprivate.csv' ), 'Encoded traversal rejected before filesystem lookup' );
bazna_archive_assert( '' === $archive->sanitize_archive_name( 'nested%2Fpublic.csv' ), 'Nested archive paths rejected rather than reduced to a basename' );
bazna_archive_assert( '' === $archive->sanitize_archive_name( 'nested%5Cpublic.csv' ), 'Windows separators rejected before filesystem lookup' );
bazna_archive_assert( '12345678903_webshop_Ulica 1_001_ROBA_1_20261117_0930.csv' === $archive->sanitize_archive_name( '12345678903_webshop_Ulica 1_001_ROBA_1_20261117_0930.csv' ), 'Legal filename with spaces remains accessible' );
$all = $archive->get_official_archive_files();
bazna_archive_assert( 4 === count( $all ), 'Only current merchant, valid named public CSV/XML snapshots listed' );
bazna_archive_assert( str_contains( $all[0]['name'], 'RI-01' ) && 'csv' === $all[0]['ext'], 'Latest official snapshot sorts first' );
bazna_archive_assert( str_contains( $all[3]['name'], '_1_20261117_0930.csv' ), 'Both original and replacement on the same day remain visible' );
bazna_archive_assert( str_contains( $all[3]['url'], 'Ulica%201' ), 'Legal file URLs encode spaces for public download' );
$ri = $archive->get_official_archive_files( 'RI-01' );
bazna_archive_assert( 1 === count( $ri ) && str_contains( $ri[0]['name'], 'RI-01_ROBA' ), 'Outlet archive filters by exact code' );
$nope = $archive->get_official_archive_files( 'FAKE' );
bazna_archive_assert( array() === $nope, 'Unknown outlet cannot acquire other official snapshots' );
$html = $archive->get_archive_embed_html( 'RI-01' );
bazna_archive_assert( str_contains( $html, 'Službeni CSV/XML cjenici' ) && str_contains( $html, 'RI-01' ), 'Official archive is included in front-end shortcode' );
bazna_archive_assert( ! str_contains( $html, '_001_' ), 'Outlet page does not mix webshop price archive' );
foreach ( $names as $name ) { unlink( $GLOBALS['bazna_archive_test_dir'] . '/' . $name ); }
rmdir( $GLOBALS['bazna_archive_test_dir'] );
echo "PASS: BAZNA immutable official public archive discovery and outlet filtering\n";
