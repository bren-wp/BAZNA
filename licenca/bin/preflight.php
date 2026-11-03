<?php
/**
 * BAZNA hosting preflight. CLI only: no HTTP exposure and no secret output.
 *
 *   php licenca/bin/preflight.php [ABSOLUTE_DOCUMENT_ROOT]
 *
 * No licence keys or secret values are ever printed or accepted as arguments.
 */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}
define('BAZNA_LICENSE_INTERNAL', true);
require_once dirname(__DIR__) . '/lib.php';

$errors = array();
$root = $argv[1] ?? dirname(__DIR__, 2);
$rootReal = realpath($root);
if (!is_string($rootReal) || !is_dir($rootReal)) {
    $errors[] = 'Neispravan dokumentni korijen (document root).';
}
if (PHP_VERSION_ID < 80100) {
    $errors[] = 'PHP mora biti najmanje 8.1.';
}
foreach (array('curl_init', 'json_encode', 'hash_hmac', 'random_bytes', 'flock') as $name) {
    if (!function_exists($name)) {
        $errors[] = 'Nedostaje PHP funkcija: ' . $name;
    }
}
$path = dirname(__DIR__) . '/private/config.php';
if (!is_file($path)) {
    $errors[] = 'Nedostaje private/config.php; API će odgovoriti HTTP 503.';
    $config = array();
} else {
    $config = require $path;
    if (!is_array($config)) {
        $config = array();
    }
}
$master = $config['master_secret_hex'] ?? null;
if (!is_string($master) || !preg_match('/^[a-f0-9]{64}$/i', $master)) {
    $errors[] = 'Privatni master ključ nije valjan (potrebne 32 nasumične bajtove).';
}
$defaultStore = dirname(__DIR__, 3) . '/bazna-license-private';
$storage = isset($config['storage_dir']) && is_string($config['storage_dir'])
    ? $config['storage_dir'] : $defaultStore;
if (!bazna_license_storage_is_private((string) $rootReal, $storage)) {
    $errors[] = 'JSON pohrana mora biti apsolutna putanja izvan javnog document root-a.';
}
$parent = realpath(dirname($storage));
$writable = is_dir($storage) ? is_writable($storage) :
    ($parent !== false && is_writable($parent));
if (!$writable) {
    $errors[] = 'PHP nema dozvolu pisanja u JSON mapu niti njen roditeljski direktorij.';
}
if ($errors) {
    fwrite(STDERR, "BAZNA licencni servis: NIJE SPREMAN\n");
    foreach ($errors as $message) {
        fwrite(STDERR, '- ' . $message . "\n");
    }
    exit(1);
}
echo "BAZNA licencni servis: LOKALNA PROVJERA USPJEŠNA\n";
echo "PHP, cURL, JSON, privatni master i putanja spremni su za provjeru na hostingu.\n";
echo "Potrebna dodatna mrežna provjera: HTTPS certifikat, REST dokaz vlasništva i zaštita privatne mape.\n";
