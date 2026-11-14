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
    'products' => [
        $oil,
        array_merge($oil, [
            'id' => 'SKU-2', 'naziv' => 'Riječka sol',
            'outlet' => 'RI-01', 'maloprodajna_cijena' => '2,49',
        ]),
    ],
    'services' => [[
        'id' => 'S-01', 'naziv' => 'Servis uređaja',
        'maloprodajna_cijena' => '45,00',
    ]],
];
$catalog['on_request'] = [
    ['id' => 'Q-1', 'naziv' => 'Izrada po mjeri',
     'kriteriji' => 'Ovisno o materijalu i dimenzijama', 'outlet' => ''],
    ['id' => 'Q-2', 'naziv' => 'Dostava prema dogovoru',
     'kriteriji' => 'Lokacija i količina', 'outlet' => 'RI-01'],
];
$badInquiry = $catalog;
$badInquiry['on_request'][0]['outlet'] = 'OTHER';
try {
    Core::validateCatalog($badInquiry);
    throw new RuntimeException('Unknown outlet was accepted in inquiry.');
} catch (InvalidArgumentException $ignored) {}
$badInquiry['on_request'][0]['kriteriji'] = '<script>alert(1)</script>';
$badInquiry['on_request'][0]['outlet'] = 'OTHER';
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
check($core->onRequestServices()['items'] === [], 'draft inquiries never visible to public');
check(str_contains($before['on_request'], '/bazna/v1/usluge-na-upit'),
    'same-origin published inquiries discovery link');
$licenseActive = $core->discovery('web', null, true);
$licenseInactive = $core->discovery('joomla', null, false);
check($licenseActive['licensing'] === ['required' => true, 'active' => true] &&
    $licenseInactive['licensing'] === ['required' => true, 'active' => false] &&
    !isset($before['licensing']),
    'same read-only Android license status as WordPress, backwards-compatible omission');
check($before['api_version'] === 1 && $before['feeds']['proizvodi']['available_csv'] === false,
    'unpublished catalog not claimed as public');
$published = $core->publish();
$requests = $core->onRequestServices();
check($requests['schema'] === 1 && count($requests['items']) === 2,
    'published request services available to all platform adapters');
check($requests['items'][0]['name'] === 'Izrada po mjeri' &&
    $requests['items'][1]['outlet'] === 'RI-01',
    'name, criteria and outlet remain available without a fabricated price');
check(!str_contains($core->csv('usluge'), 'Ovisno o materijalu'),
    'inquiry-only services are excluded from numeric CSV');
check(!str_contains($core->xml('usluge'), 'Izrada po mjeri'),
    'inquiry-only services are excluded from numeric XML');
check(isset($published['published_at']), 'publication timestamp');
$index = $core->discovery('joomla');
// This is a source-level contract check; no WP bootstrap, database or live
// website is needed. It must fail whenever one platform drifts legally.
if (!defined('ABSPATH')) define('ABSPATH', '/offline-contract-test/');
require_once dirname(__DIR__, 2) . '/wordpress/brendigo-sidrena-cijena/includes/class-legal.php';
$wpLegal = Brendigo_Sidrena_Cijena_Legal::public_contract();
$commonLegal = $core->legal();
foreach ([
    'schema', 'jurisdiction', 'rules_revision', 'consumer_law_revision',
    'unit_price_rule_revision', 'unit_price_rule_effective_from',
    'price_reduction_lookback_days', 'discount_baseline_is_anchor_price',
    'base_price_regulation_status', 'base_price_rule_effective_from',
    'final_rulebooks_verified', 'reviewed_on', 'timezone',
    'base_price_is_automatically_derived', 'historical_price_verification_required',
    'legal_compliance_certified', 'consumer_30_day_low_is_anchor_price',
    'consumer_30_day_low_is_base_price', 'draft_rulebooks_consultation_until',
    'effective_from', 'reference_dates', 'legacy_categories',
    'product_publication_deadline', 'service_change_deadline',
    'archive_min_days', 'formats', 'products_required_fields',
    'services_required_fields', 'classification', 'historical_prices',
] as $field) {
    check(($commonLegal[$field] ?? null) === ($wpLegal[$field] ?? null),
        'WordPress/PHP/Joomla/Shopify legal contract drift on ' . $field);
}
foreach ([
    'published_decisions_ready_from', 'reference_price_data_entry',
    'product_service_publication', 'unpublished_draft_features',
    'discount_30_day_low_independent', 'automatic_historical_inference',
] as $field) {
    check(($commonLegal['implementation'][$field] ?? null) ===
        ($wpLegal['implementation'][$field] ?? null),
        'platform legal implementation drift on ' . $field);
}
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
check(count($archive) === 4 && $archive[0]['kind'] === 'proizvodi',
    'webshop plus branch-specific archived CSV formats');
$branchArchive = array_values(array_filter($archive, static fn($entry) =>
    $entry['kind'] === 'proizvodi' && $entry['outlet'] === 'RI-01'));
check(count($branchArchive) === 1 &&
    str_contains($branchArchive[0]['url'], '?oznaka=RI-01'),
    'archived branch link retains its own public business code');
preg_match('#/arhiva/([0-9]{14}-[a-f0-9]{12})/proizvodi/csv$#', $archive[0]['url'], $matches);
check(isset($matches[1]) && str_contains($core->archiveCsv($matches[1], 'proizvodi'), '5,99'),
    'immutable previous published price');
check(str_contains($core->csv('proizvodi'), '6,19'), 'current publication newer');
check(str_contains($core->archiveCsv($matches[1], 'proizvodi', 'RI-01'), 'Riječka sol'),
    'published branch prices preserved in immutable archive even after removal');
try {
    $core->archiveCsv($matches[1], 'proizvodi', 'NOT-AN-OUTLET');
    throw new RuntimeException('Foreign branch archive accepted.');
} catch (InvalidArgumentException $ignored) {}
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
