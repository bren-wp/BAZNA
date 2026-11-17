<?php
/**
 * NN 132/2026 article 3(6): keep each replaced public CSV/XML for 15 days
 * from replacement, including real addresses with spaces/diacritics.
 * This test uses a temporary isolated directory, not merchant data.
 */
declare(strict_types=1);
define( 'ABSPATH', __DIR__ . '/' );
define( 'DAY_IN_SECONDS', 86400 );

$GLOBALS['bazna_retention_dir'] = sys_get_temp_dir() . '/bazna-retention-' . bin2hex( random_bytes( 6 ) );
$GLOBALS['bazna_retention_holds'] = array();
mkdir( $GLOBALS['bazna_retention_dir'] . '/sidrena-cijena', 0700, true );

function retention_check( bool $ok, string $label ): void {
    if ( ! $ok ) {
        fwrite( STDERR, "FAIL: $label\n" );
        exit( 1 );
    }
}
function wp_upload_dir( ...$args ) {
    return array( 'basedir' => $GLOBALS['bazna_retention_dir'], 'error' => false );
}
function trailingslashit( $path ) { return rtrim( $path, '/\\' ) . '/'; }
function get_option( $key, $default = null ) {
    return 'bazna_official_retention_holds' === $key ? $GLOBALS['bazna_retention_holds'] : $default;
}
function update_option( $key, $value, $autoload = false ) {
    if ( 'bazna_official_retention_holds' === $key ) {
        $GLOBALS['bazna_retention_holds'] = $value;
    }
    return true;
}
class Brendigo_Sidrena_Cijena_Filesystem {
    public static function dirlist( $dir ) {
        if ( ! is_dir( $dir ) ) { return array(); }
        $result = array();
        foreach ( scandir( $dir ) as $name ) {
            if ( '.' !== $name && '..' !== $name ) {
                $result[ $name ] = array( 'type' => is_file( $dir . '/' . $name ) ? 'f' : 'd' );
            }
        }
        return $result;
    }
    public static function delete( $path ) { return unlink( $path ); }
}

require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-feed-files.php';
$feed = Brendigo_Sidrena_Cijena_Feed_Files::instance();
$now = time();
$stamp = static function ( int $seconds ): string {
    return ( new DateTimeImmutable( '@' . $seconds ) )
        ->setTimezone( new DateTimeZone( 'Europe/Zagreb' ) )->format( 'Ymd_Hi' );
};
$old = '12345678903_prodavaonica_Korzo 12_RI-01_ROBA_7_' . $stamp( $now - 45 * DAY_IN_SECONDS ) . '.csv';
$expired = '12345678903_webshop_Ulica 1_001_USLUGE_8_' . $stamp( $now - 42 * DAY_IN_SECONDS ) . '.xml';
$unheld = '12345678903_prodavaonica_Korzo 14_RI-02_ROBA_9_' . $stamp( $now - 40 * DAY_IN_SECONDS ) . '.csv';
$recent = '12345678903_prodavaonica_Žrtava 8_RI-03_ROBA_10_' . $stamp( $now - 3 * DAY_IN_SECONDS ) . '.csv';
$bad = 'sidrena-cijena.csv';
$dir = $GLOBALS['bazna_retention_dir'] . '/sidrena-cijena';
foreach ( array( $old, $expired, $unheld, $recent, $bad ) as $name ) {
    file_put_contents( $dir . '/' . $name, 'public fixture' );
}
$GLOBALS['bazna_retention_holds'] = array(
    $old => $now + 14 * DAY_IN_SECONDS, // Replaced recently, despite old publish date.
    $expired => $now - 1, // The 15-day replacement grace period has elapsed.
);
retention_check( Brendigo_Sidrena_Cijena_Feed_Files::official_snapshot_name_is_valid( $old ), 'Address spaces are valid official filenames' );
retention_check( Brendigo_Sidrena_Cijena_Feed_Files::official_snapshot_name_is_valid( $recent ), 'Croatian characters are valid official filenames' );
retention_check( ! Brendigo_Sidrena_Cijena_Feed_Files::official_snapshot_name_is_valid( '../' . $old ), 'Traversal cannot be treated as official filename' );
retention_check( ! Brendigo_Sidrena_Cijena_Feed_Files::official_snapshot_name_is_valid( $bad ), 'Unrelated uploads are never official snapshots' );
$method = new ReflectionMethod( Brendigo_Sidrena_Cijena_Feed_Files::class, 'prune_official_snapshots' );
$method->invoke( $feed );
retention_check( file_exists( $dir . '/' . $old ), 'Held 45-day-old snapshot remains public for 15 days after replacement' );
retention_check( ! file_exists( $dir . '/' . $expired ), 'Expired old official file can be pruned' );
retention_check( ! file_exists( $dir . '/' . $unheld ), 'Unheld old official file can be pruned' );
retention_check( file_exists( $dir . '/' . $recent ), 'Recently published Croatian-address snapshot remains' );
retention_check( file_exists( $dir . '/' . $bad ), 'Unrelated public files are not deleted' );
retention_check( isset( $GLOBALS['bazna_retention_holds'][ $old ] ) &&
    ! isset( $GLOBALS['bazna_retention_holds'][ $expired ] ), 'Unexpired hold persists, expired hold expires' );
foreach ( scandir( $dir ) as $name ) {
    if ( '.' !== $name && '..' !== $name ) { unlink( $dir . '/' . $name ); }
}
rmdir( $dir );
rmdir( $GLOBALS['bazna_retention_dir'] );
echo "PASS: NN132 official archive pruning respects real filenames and replacement holds\n";
