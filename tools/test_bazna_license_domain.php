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

// A transient remote-service outage must never immediately revoke a
// previously verified license. Only explicit domain/key denials may do so.
foreach (array(
    array('ok' => false, 'code' => 'rate_limit_exceeded'),
    array('ok' => false, 'code' => 'service_unreachable'),
    array('ok' => false, 'code' => 'license_service_unavailable'),
    array('ok' => false, 'code' => 'invalid_service_reply'),
    array('ok' => false, 'code' => 'unsafe_storage_path'),
) as $temporary) {
    bazna_assert(!$license::is_authoritative_denial($temporary),
        'remote outage must not revoke license: ' . $temporary['code']);
}
foreach (array('invalid_key', 'not_activated_for_domain',
    'already_used_on_another_domain') as $reason) {
    bazna_assert($license::is_authoritative_denial(array('ok' => false, 'code' => $reason)),
        'explicit denial revokes license: ' . $reason);
}
bazna_assert($license::is_valid_active_reply(array(
    'ok' => true, 'code' => 'active', 'domain' => 'primjer.hr',
)), 'active reply for own domain');
bazna_assert(!$license::is_valid_active_reply(array(
    'ok' => true, 'code' => 'active', 'domain' => 'druga.hr',
)), 'cannot activate from response for second domain');
bazna_assert(!$license::is_valid_active_reply(array(
    'ok' => true, 'code' => 'not_activated_for_domain', 'domain' => 'primjer.hr',
)), 'success flag alone cannot activate');

// A lapsed license must block premium Woo product writes while consumers
// can still view the previously published reference price.
require __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-fields.php';
class BaznaLockedTestProduct {
    public function is_type($type) { return false; }
    public function get_id() { return 42; }
}
$_POST['sidrena_cijena'] = '9999.99';
Brendigo_Sidrena_Cijena_Fields::save_simple(new BaznaLockedTestProduct());
Brendigo_Sidrena_Cijena_Fields::save_variation(42, 0);
unset($_POST['sidrena_cijena']);
$pluginSource = file_get_contents(__DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-plugin.php');
$publicInit = strpos($pluginSource, 'Brendigo_Sidrena_Cijena_Woo_Blocks::instance();');
$licenceGate = strpos($pluginSource, "if ( ! Brendigo_Sidrena_Cijena_License::valid() || ! self::woocommerce_active() )");
bazna_assert($publicInit !== false && $licenceGate !== false && $publicInit < $licenceGate,
    'Woo public pricing remains available before the administration license gate');

$source = file_get_contents(__DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-admin.php');
bazna_assert(strpos($source, 'name="bazna_license_domain"') !== false, 'activation form has domain input');
bazna_assert(strpos($source, 'name="bazna_license_key"') !== false, 'activation form has license key input');
echo "PASS: BAZNA licence domain input, canonical binding, REST paths, offline expiry and outage-safe license refresh and locked Woo editing\n";
