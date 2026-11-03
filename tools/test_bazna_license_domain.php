<?php
/** Self-contained WordPress domain/licence regression tests, no HTTP or SQL. */
define('ABSPATH', __DIR__ . '/');
define('DAY_IN_SECONDS', 86400);
$GLOBALS['bazna_home'] = 'https://www.primjer.hr/';
$GLOBALS['bazna_rest_root'] = 'https://www.primjer.hr/wordpress/wp-json/';
$GLOBALS['bazna_options'] = array();

function home_url($path = '/') { return $GLOBALS['bazna_home']; }
function rest_url($route) { return $GLOBALS['bazna_rest_root'] . $route; }
function wp_parse_url($url, $component) { return parse_url($url, $component); }
function trailingslashit($value) { return rtrim($value, '/') . '/'; }
function get_option($key, $fallback = false) {
    return array_key_exists($key, $GLOBALS['bazna_options'])
        ? $GLOBALS['bazna_options'][$key] : $fallback;
}
function bazna_assert($condition, $message) {
    if (!$condition) {
        fwrite(STDERR, 'FAIL: ' . $message . PHP_EOL);
        exit(1);
    }
}
require __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-license.php';
$license = 'Brendigo_Sidrena_Cijena_License';

bazna_assert($license::canonical_domain('www.Primjer.hr') === 'primjer.hr', 'www alias');
bazna_assert($license::canonical_domain('primjer.hr') === 'primjer.hr', 'bare domain');
bazna_assert($license::canonical_domain('sub.primjer.hr') === 'sub.primjer.hr', 'subdomain separate');
foreach (array('https://primjer.hr', 'primjer.hr/path', 'primjer.hr:443',
    '192.168.1.1', 'localhost', 'primjer.hr@other.hr', 'druga.hr') as $input) {
    bazna_assert(!$license::domain_matches_site($input), 'reject foreign/malformed domain ' . $input);
}
bazna_assert($license::domain_matches_site('primjer.hr'), 'accept exact domain without www');
bazna_assert($license::domain_matches_site('WWW.PRIMJER.HR'), 'accept actual www host');
bazna_assert($license::site_path() === '/wordpress/', 'use WordPress REST installation path');
$GLOBALS['bazna_rest_root'] = 'https://www.primjer.hr/wordpress/?rest_route=/';
bazna_assert($license::site_path() === '/wordpress/', 'plain permalink subdirectory');
$GLOBALS['bazna_rest_root'] = 'https://www.primjer.hr/wordpress/wp-json/';

$GLOBALS['bazna_options'][$license::OPTION] = array(
    'active' => true, 'host' => 'primjer.hr', 'checked' => time(),
    'encrypted_key' => 'not-the-real-key',
);
bazna_assert($license::valid(), 'one canonical domain unlocks original site');
$GLOBALS['bazna_home'] = 'https://druga.hr/';
bazna_assert(!$license::valid(), 'copied WordPress options must not unlock a second domain');
$GLOBALS['bazna_home'] = 'https://www.primjer.hr/';
$GLOBALS['bazna_options'][$license::OPTION]['checked'] = time() - 4 * DAY_IN_SECONDS;
bazna_assert(!$license::valid(), 'offline verification expires after grace period');

$source = file_get_contents(__DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-admin.php');
bazna_assert(strpos($source, 'name="bazna_license_domain"') !== false, 'activation form has domain input');
bazna_assert(strpos($source, 'name="bazna_license_key"') !== false, 'activation form has license key input');
echo "PASS: BAZNA licence domain input, canonical binding, REST paths and offline expiry\n";
