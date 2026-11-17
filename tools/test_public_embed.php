<?php
/**
 * BAZNA WordPress embed contract: real shortcode callbacks with minimal WP stubs.
 * Exercises theme sanitization, official-file discovery, outlet filtering, and
 * draft/stale-state handling without a database, HTTP, or private user data.
 */
declare(strict_types=1);
define( 'ABSPATH', __DIR__ . '/' );
define( 'BRENDIGO_SIDRENA_CIJENA_VERSION', '1.5.24' );
define( 'BRENDIGO_SIDRENA_CIJENA_URL', 'https://example.test/wp-content/plugins/brendigo-sidrena-cijena/' );
$GLOBALS['bazna_shortcodes'] = array();
$GLOBALS['bazna_prefs'] = array( 'default_date' => '10.09.2026.', 'public_theme' => 'light', 'public_accent' => 'blue', 'public_density' => 'compact' );
$GLOBALS['bazna_official_files'] = array(
    'official_feeds' => array(
        'proizvodi' => array( 'csv' => 'https://example.test/public/roba.csv', 'xml' => 'https://example.test/public/roba.xml' ),
        'usluge' => array( 'csv' => '', 'xml' => '' ),
    ),
    'outlets' => array(
        array(
            'code' => 'RI-01',
            'official_feeds' => array(
                'proizvodi' => array( 'csv' => 'https://example.test/public/RI-01-roba.csv', 'xml' => 'https://example.test/public/RI-01-roba.xml' ),
                'usluge' => array( 'csv' => 'https://example.test/public/RI-01-usluge.csv', 'xml' => '' ),
            ),
        ),
    ),
    'publication' => array( 'pending_changes' => true ),
);
function bazna_embed_check( bool $ok, string $name ): void {
    if ( ! $ok ) {
        fwrite( STDERR, "FAIL: $name\n" );
        exit( 1 );
    }
}
function get_option( $key, $default = null ) {
    return 'sidrena_cijena_settings' === $key ? $GLOBALS['bazna_prefs'] : $default;
}
function wp_parse_args( $stored, $defaults ) { return array_merge( $defaults, $stored ); }
function sanitize_text_field( $value ) { return trim( strip_tags( (string) $value ) ); }
function sanitize_key( $value ) { return preg_replace( '/[^a-z0-9_-]/', '', strtolower( (string) $value ) ); }
function esc_attr( $value ) { return htmlspecialchars( (string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8' ); }
function esc_html( $value ) { return esc_attr( $value ); }
function esc_url( $url ) { return str_starts_with( (string) $url, 'https://' ) ? esc_attr( $url ) : ''; }
function esc_html__( $msg, $domain ) { return esc_html( $msg ); }
function esc_attr__( $msg, $domain ) { return esc_attr( $msg ); }
function __( $msg, $domain ) { return $msg; }
function shortcode_atts( $defaults, $atts, $name = '' ) { return array_merge( $defaults, is_array( $atts ) ? $atts : array() ); }
function add_action( ...$args ) {}
function add_shortcode( $name, $callback ) { $GLOBALS['bazna_shortcodes'][ $name ] = $callback; }
function wp_style_is( ...$args ) { return false; }
function wp_register_style( ...$args ) {}
function wp_enqueue_style( ...$args ) {}
function wp_print_styles( ...$args ) {}
function did_action( ...$args ) { return 0; }
function wp_script_is( ...$args ) { return false; }
function wp_register_script( ...$args ) {}
function wp_enqueue_script( ...$args ) {}
class Brendigo_Sidrena_Cijena_Plugin {
    public static function woocommerce_active() { return false; }
}
class Brendigo_Sidrena_Cijena_Poslovnice {
    public static function instance() { return new self(); }
    public function get_by_oznaka( $code ) { return 'RI-01' === $code ? array( 'oznaka' => $code ) : null; }
}
class Brendigo_Sidrena_Cijena_Export {
    public static function instance() { return new self(); }
    public function get_cjenik_embed_html( $code ) { return '<div class="sidrena-cjenik">PUB-ONLY</div>'; }
    public function get_archive_embed_html( $code ) { return '<ol class="sidrena-arhiva"><li>ARCHIVE-ONLY</li></ol>'; }
}
class Bazna_Response {
    public function get_data() { return $GLOBALS['bazna_official_files']; }
}
class Brendigo_Sidrena_Cijena_Public_API {
    public static function instance() { return new self(); }
    public function index() { return new Bazna_Response(); }
}
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-settings.php';
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-frontend.php';
$settings = Brendigo_Sidrena_Cijena_Settings::instance();
bazna_embed_check( 'light' === Brendigo_Sidrena_Cijena_Settings::safe_public_option( 'light', 'theme' ), 'Light theme accepted' );
bazna_embed_check( 'dark' === Brendigo_Sidrena_Cijena_Settings::safe_public_option( '<script>', 'theme' ), 'Invalid markup theme rejected' );
bazna_embed_check( 'orange' === Brendigo_Sidrena_Cijena_Settings::safe_public_option( 'url(javascript:evil)', 'accent' ), 'CSS injection rejected' );
bazna_embed_check( '' === Brendigo_Sidrena_Cijena_Settings::safe_public_option( 'blue', 'unknown' ), 'Unknown appearance dimension ignored' );
bazna_embed_check( str_contains( $settings->public_appearance_classes(), 'bazna-theme-light bazna-accent-blue bazna-density-compact' ), 'Stored choices produce only known CSS classes' );
$frontend = Brendigo_Sidrena_Cijena_Frontend::instance();
foreach ( array( 'bazna_objava', 'bazna-objava', 'bazna_cjenik', 'bazna_arhiva', 'bazna_datoteke' ) as $tag ) {
    bazna_embed_check( isset( $GLOBALS['bazna_shortcodes'][ $tag ] ), "Shortcode registered: $tag" );
}
$files = $frontend->bazna_official_files_shortcode( array() );
bazna_embed_check( str_contains( $files, 'roba.csv' ) && str_contains( $files, 'roba.xml' ), 'Only confirmed official goods URLs are linked' );
bazna_embed_check( ! str_contains( $files, 'RI-01' ) && ! str_contains( $files, 'usluge.csv' ), 'Never leak outlet official files into general catalogue' );
bazna_embed_check( str_contains( $files, 'Promjene cijena čekaju ponovnu objavu' ), 'Pending publication is not advertised as current' );
$per_outlet = $frontend->bazna_official_files_shortcode( array( 'oznaka' => 'RI-01', 'vrsta' => 'usluge' ) );
bazna_embed_check( str_contains( $per_outlet, 'RI-01-usluge.csv' ) && ! str_contains( $per_outlet, 'roba.csv' ), 'Only relevant confirmed outlet/service CSV linked' );
$invalid = $frontend->bazna_official_files_shortcode( array( 'oznaka' => 'UNKNOWN' ) );
bazna_embed_check( str_contains( $invalid, 'nije pronađeno' ) && ! str_contains( $invalid, 'roba.csv' ), 'Unknown outlet is rejected' );
$full = $frontend->bazna_publication_shortcode( array() );
bazna_embed_check( str_contains( $full, 'PUB-ONLY' ) && str_contains( $full, 'ARCHIVE-ONLY' ) && str_contains( $full, 'roba.csv' ), 'Combined shortcode renders public table, archive, official file links' );
$no_archive = $frontend->bazna_publication_shortcode( array( 'arhiva' => 'ne' ) );
bazna_embed_check( ! str_contains( $no_archive, 'ARCHIVE-ONLY' ) && str_contains( $no_archive, 'roba.csv' ), 'Archive can be hidden without hiding official files' );
$GLOBALS['bazna_official_files']['official_feeds'] = array();
$unpublished = $frontend->bazna_publication_shortcode( array() );
bazna_embed_check( str_contains( $unpublished, 'još nisu potvrđeno objavljene' ) && ! str_contains( $unpublished, 'class="bazna-file-link"' ), 'Unpublished official files produce a warning, not fake public links' );
$css = file_get_contents( __DIR__ . '/../wordpress/brendigo-sidrena-cijena/assets/css/public-appearance.css' );
bazna_embed_check( is_string( $css ) && str_contains( $css, '.bazna-publication' ) && str_contains( $css, ':focus-visible' ), 'Scoped and accessible appearance CSS included' );
echo "PASS: BAZNA shortcodes, safe appearance and verified public CSV/XML discovery\n";
