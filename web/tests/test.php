<?php
/** Offline BAZNA multi-platform PHP catalog, publication and archive contract. */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require_once dirname(__DIR__) . '/src/Core.php';
require_once dirname(__DIR__) . '/src/LicenseClient.php';
use Brendigo\Bazna\Core;
use Brendigo\Bazna\LicenseClient;

function check(bool $ok, string $message): void {
    if (!$ok) throw new RuntimeException('BAZNA web test failed: ' . $message);
}
$root = sys_get_temp_dir() . '/bazna-platform-' . bin2hex(random_bytes(5));
$storage = $root . '/private';
$webroot = $root . '/public';
mkdir($root, 0700);
mkdir($webroot, 0700);
$_SERVER['DOCUMENT_ROOT'] = $webroot;
$core = new Core($storage, 'https://primjer.hr');
$core->ensurePrivate();
check(is_dir($storage), 'private folder created');
try {
    (new Core($webroot . '/bad', 'https://primjer.hr'))->ensurePrivate();
    throw new RuntimeException('Public storage was accepted.');
} catch (RuntimeException $e) {
    check(str_contains($e->getMessage(), 'DOCUMENT_ROOT'), 'public storage rejected');
}
try {
    new Core($storage, 'http://primjer.hr');
    throw new RuntimeException('HTTP site was accepted.');
} catch (InvalidArgumentException $ignored) {}
$oil = [
    'id' => 'SKU-1', 'naziv' => 'Maslinovo ulje', 'sku' => 'MU-1',
    'maloprodajna_cijena' => '5,99', 'sidrena_cijena' => '8,49',
    'sidrena_datum' => '2026-09-10', 'historical_verified' => true,
    'jedinica_mjere' => 'L', 'outlet' => '',
];
$catalog = [
    'outlets' => [['code' => 'RI-01', 'name' => 'Rijeka', 'address' => 'Korzo 12, Rijeka']],
    'products' => [$oil],
    'services' => [[
        'id' => 'S-01', 'naziv' => 'Servis uređaja',
        'maloprodajna_cijena' => '45,00',
    ]],
];
$bad = $catalog;
$bad['products'][0]['historical_verified'] = false;
try {
    $core->saveDraft($bad);
    throw new RuntimeException('Unverified historical price was accepted.');
} catch (InvalidArgumentException $ignored) {}
$bad = $catalog;
$bad['products'][0]['outlet'] = 'UNKNOWN';
try {
    $core->saveDraft($bad);
    throw new RuntimeException('Unregistered outlet was accepted.');
} catch (InvalidArgumentException $ignored) {}
$core->saveDraft($catalog);
$before = $core->discovery('web');
check($before['api_version'] === 1 && $before['feeds']['proizvodi']['available_csv'] === false,
    'unpublished catalog not claimed as public');
$published = $core->publish();
check(isset($published['published_at']), 'publication timestamp');
$index = $core->discovery('joomla');
check($index['feeds']['proizvodi']['available_csv'] === true &&
    $index['outlets'][0]['address'] === 'Korzo 12, Rijeka', 'public outlet discovery');
check($index['platform'] === 'joomla', 'platform name');
check($index['legal']['effective_from'] === '2026-11-17' &&
    $index['legal']['historical_price_verification_required'] === true, 'legal contract');
check($index['publication']['compliance_certified'] === false, 'no false legal certification');
check(!array_key_exists('key', $index), 'no public license keys');
$csv = $core->csv('proizvodi');
check(str_contains($csv, 'sidrena_cijena') && str_contains($csv, 'Maslinovo ulje'),
    'legacy Android CSV columns compatible with BAZNA user branding');
$lines = array_map('str_getcsv', explode("\n", trim(substr($csv, 3))));
check(str_contains($csv, "\xEF\xBB\xBF"), 'UTF-8 BOM');
$xml = $core->xml('proizvodi');
check(str_contains($xml, '<bazna-cjenik') && str_contains($xml, '<sidrena_cijena>8,49</sidrena_cijena>'),
    'XML and CSV price parity');
$core->saveDraft([
    'outlets' => $catalog['outlets'],
    'products' => [[
        'id' => 'SKU-1', 'naziv' => 'Maslinovo ulje',
        'maloprodajna_cijena' => '6,19', 'sidrena_cijena' => '8,49',
        'sidrena_datum' => '2026-09-10', 'historical_verified' => true,
    ]],
    'services' => $catalog['services'],
]);
$core->publish();
$archive = $core->archives();
check(count($archive) === 2 && $archive[0]['kind'] === 'proizvodi', 'two archived formats');
preg_match('#/arhiva/([0-9]{14}-[a-f0-9]{12})/proizvodi/csv$#', $archive[0]['url'], $matches);
check(isset($matches[1]) && str_contains($core->archiveCsv($matches[1], 'proizvodi'), '5,99'),
    'immutable previous published price');
check(str_contains($core->csv('proizvodi'), '6,19'), 'current publication newer');
$core->publish();
check(str_contains($core->archiveCsv($matches[1], 'proizvodi'), '5,99'),
    'archive link remains stable after another publication');
$license = new LicenseClient($core, str_repeat('ab', 32));
check(!$license->active() && $license->proof() === null, 'unactivated license cannot unlock local edits');
try {
    new LicenseClient($core, 'unsafe-key');
    throw new RuntimeException('Invalid encryption secret accepted.');
} catch (RuntimeException $ignored) {}

$remove = static function ($dir) use (&$remove): void {
    foreach (scandir($dir) ?: [] as $part) {
        if ($part === '.' || $part === '..') continue;
        $p = $dir . '/' . $part;
        if (is_dir($p)) $remove($p); else unlink($p);
    }
    rmdir($dir);
};
$remove($root);
echo "PASS: BAZNA cross-platform catalog, historical validation, immutable archive, public v1, HTTPS and license guard\n";
