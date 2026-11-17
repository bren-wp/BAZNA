<?php
/** Offline BAZNA multi-platform PHP catalog, publication and archive contract. */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require_once dirname(__DIR__) . '/src/Core.php';
use Brendigo\Bazna\Core;

function check(bool $ok, string $message): void {
    if (!$ok) throw new RuntimeException('BAZNA web test failed: ' . $message);
}
$root = sys_get_temp_dir() . '/bazna-platform-' . bin2hex(random_bytes(5));
$storage = $root . '/private';
$webroot = $root . '/public';
mkdir($root, 0700);
mkdir($webroot, 0700);
$_SERVER['DOCUMENT_ROOT'] = $webroot;
// Automated publication must be strictly idempotent, with fully documented data.
// The separate test tenant protects existing archive/regression fixture counts.
$automated = new Core($root . '/automatic', 'https://primjer.hr');
$automated->ensurePrivate();
$autoProduct = [
    'id' => 'AUTO-P1', 'naziv' => 'Novo verificirano dobro',
    'maloprodajna_cijena' => '5,00', 'sidrena_cijena' => '4,00',
    'sidrena_datum' => '2026-09-10', 'historical_verified' => true, 'historical_source' => 'Arhiva cjenika 2026', 'reference_scope' => 'general_goods_services', 'historical_method' => 'reference_day',
];
$autoService = [
    'id' => 'AUTO-S1', 'naziv' => 'Usluga',
    'maloprodajna_cijena' => '15,00',
];
$automated->saveDraft([
    'products' => [$autoProduct], 'services' => [$autoService],
    'outlets' => [], 'on_request' => [],
]);
putenv('BAZNA_TRADER_OIB=12345678903');
try {
    $automated->publish(true, true);
    throw new RuntimeException('Incomplete automatic official publication was accepted.');
} catch (RuntimeException $e) {
    check(str_contains($e->getMessage(), 'nedostaje'), 'Missing base price blocks automatic publication');
}
check(!isset($automated->read('published.json')['published_at']), 'Rejected automatic publish does not leak drafts');
$autoService['sidrena_cijena'] = '14,00';
$autoService['sidrena_datum'] = '2026-09-10';
$autoService['historical_verified'] = true;
$autoService['historical_source'] = 'Arhiva usluga 2026';
$autoService['reference_scope'] = 'general_goods_services';
$autoService['historical_method'] = 'reference_day';
$automated->saveDraft([
    'products' => [$autoProduct], 'services' => [$autoService],
    'outlets' => [], 'on_request' => [],
]);
$autoFirst = $automated->publish(true, true);
$autoAgain = $automated->publish(true, true);
check($autoFirst['publication_sequence'] === 1 &&
    $autoAgain['publication_sequence'] === 1 &&
    $automated->archives() === [],
    'Repeated scheduled publication is a no-op without new archive');
$autoService['maloprodajna_cijena'] = '16,00';
$automated->saveDraft([
    'products' => [$autoProduct], 'services' => [$autoService],
    'outlets' => [], 'on_request' => [],
]);
$autoSecond = $automated->publish(true, true);
check($autoSecond['publication_sequence'] === 2 &&
    count($automated->archives()) > 0,
    'Real verified change automatically publishes and archives replaced version');
putenv('BAZNA_TRADER_OIB');
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
    'sidrena_datum' => '2026-09-10', 'historical_verified' => true, 'historical_source' => 'Arhiva cjenika 2026', 'reference_scope' => 'general_goods_services', 'historical_method' => 'reference_day',
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
$core->saveDraft($bad);
check($core->read('draft.json')['products'][0]['historical_verified'] === false,
    'Unverified historical amounts may be edited in a private draft');
try {
    $core->publish();
    throw new RuntimeException('Unverified historical draft became public.');
} catch (RuntimeException $e) {
    check(str_contains($e->getMessage(), 'historical_verified'),
        'An unverified private draft cannot be published');
}
check(!isset($core->read('published.json')['published_at']),
    'Failed publication keeps unverified draft private');
$bad = $catalog;
$bad['products'][0]['outlet'] = 'UNKNOWN';
try {
    $core->saveDraft($bad);
    throw new RuntimeException('Unregistered outlet was accepted.');
} catch (InvalidArgumentException $ignored) {}
$core->saveDraft($catalog);
// A bulk JSON replacement cannot silently carry a verified flag when the
// historical amount was changed, although current sale price may change.
$changedReference = $catalog;
$changedReference['products'][0]['sidrena_cijena'] = '9,99';
$core->saveDraft($changedReference);
check($core->read('draft.json')['products'][0]['historical_verified'] === false,
    'Changed historic amount invalidates a previously approved source');
$unverifiedRestored = $catalog;
$unverifiedRestored['products'][0]['historical_verified'] = false;
$core->saveDraft($unverifiedRestored);
check($core->read('draft.json')['products'][0]['historical_verified'] === false,
    'Restoring old amount without explicit re-attestation remains unverified');
// The operator must explicitly re-attest the source after a change.
$reconfirmed = $catalog;
$reconfirmed['products'][0]['historical_verified'] = true;
$core->saveDraft($reconfirmed);
check($core->read('draft.json')['products'][0]['historical_verified'] === true,
    'Explicit re-attestation is accepted when the documented source is complete');
// Legacy manual publishing may not bypass official documentary evidence.
$unverifiedMethod = $catalog;
$unverifiedMethod['products'][0]['historical_method'] = '';
$core->saveDraft($unverifiedMethod);
check($core->read('draft.json')['products'][0]['historical_verified'] === false,
    'Editing the legal method invalidates the old historical attestation');
try {
    $core->publish();
    throw new RuntimeException('Manual publication accepted an undocumented legal method.');
} catch (RuntimeException $e) {
    check(str_contains($e->getMessage(), 'historical_verified'),
        'Manual public feed rejects the revoked attestation');
}
check(!isset($core->read('published.json')['published_at']),
    'Manual publication failure preserves the last published state');
$core->saveDraft($catalog);
$before = $core->discovery('web');
check(str_contains($core->publicCatalogHtml('proizvodi'), 'još nije objavljen'),
    'unpublished data cannot be rendered on public HTML');
check($core->onRequestServices()['items'] === [], 'draft inquiries never visible to public');
check(str_contains($before['on_request'], '/bazna/v1/usluge-na-upit'),
    'same-origin published inquiries discovery link');
check(!array_key_exists('licensing', $before), 'public index has no activation field');
check($core->discovery('joomla')['api_version'] === 1 &&
    $core->discovery('shopify')['api_version'] === 1,
    'All adapters provide public REST without license activation');
check($before['api_version'] === 1 && $before['feeds']['proizvodi']['available_csv'] === false,
    'unpublished catalog not claimed as public');
// The original publication's legal identity is immutable even when environment changes.
putenv('BAZNA_TRADER_OIB=12345678903');
$published = $core->publish();
$requests = $core->onRequestServices();
check($requests['schema'] === 1 && count($requests['items']) === 2,
    'published request services available to all platform adapters');
check($requests['items'][0]['name'] === 'Izrada po mjeri' &&
    $requests['items'][1]['outlet'] === 'RI-01',
    'name, criteria and outlet remain available without a fabricated price');
check(count($core->onRequestServices('', 'MATERIJALU dimenzijama')['items']) === 1,
    'inquiry REST supports multi-term criteria filtering');
check(count($core->onRequestServices('RI-01', 'dostava')['items']) === 1 &&
    $core->onRequestServices('RI-01', 'dostava')['items'][0]['name'] === 'Dostava prema dogovoru',
    'inquiry REST filters by business code');
check($core->onRequestServices('RI-01', 'materijalu')['items'] === [],
    'inquiry REST prevents cross-branch mixing');
try {
    $core->onRequestServices('', str_repeat('X', 200));
    throw new RuntimeException('Excessively long inquiry REST search was accepted.');
} catch (InvalidArgumentException $ignored) {}

check(!str_contains($core->csv('usluge'), 'Ovisno o materijalu'),
    'inquiry-only services are excluded from numeric CSV');
check(!str_contains($core->xml('usluge'), 'Izrada po mjeri'),
    'inquiry-only services are excluded from numeric XML');
check(isset($published['published_at']), 'publication timestamp');
check(Core::validOib('12345678903') && !Core::validOib('12345678901'), 'OIB checksum enforced');
check(Core::validEan('0123456789012') && !Core::validEan('0123456789011'),
    'EAN13 checksum enforced with leading zero');
putenv('BAZNA_TRADER_OIB=12345678903');
check($core->officialIdentity() === '12345678903', 'Explicit valid legal identity');
check(count($core->officialMissing('usluge')) === 1, 'Incomplete service history detected');
try {
    $core->officialXml('usluge');
    throw new RuntimeException('Incomplete official service feed falsely published.');
} catch (RuntimeException $ignored) {}
$official = $core->officialCsv('proizvodi');
check(str_contains($official, 'neto_kolicina;jedinica_mjere;maloprodajna_cijena;poseban_oblik_prodaje'),
    'NN132 official goods column order and names');
check(!str_contains($official, 'sidrena_datum') && !str_contains($official, 'dostupnost'),
    'Legacy compatibility-only columns omitted from official CSV');
check(str_contains($core->officialXml('proizvodi'), '<bazna_cijena>8,49</bazna_cijena>'),
    'NN132 official XML holds verified original historical price');
putenv('BAZNA_MAIN_STORE_TYPE=webshop');
putenv('BAZNA_MAIN_STORE_ADDRESS=Rijeka');
putenv('BAZNA_MAIN_STORE_CODE=001');
check(str_contains($core->officialFilename('proizvodi', 'csv'), 'webshop_rijeka_001_ROBA_'),
    'Different-outlet file includes actual main store type/address/code');
putenv('BAZNA_MAIN_STORE_TYPE');
putenv('BAZNA_MAIN_STORE_ADDRESS');
putenv('BAZNA_MAIN_STORE_CODE');
putenv('BAZNA_TRADER_OIB');

$htmlGoods = $core->publicCatalogHtml('proizvodi', '', 'maslinovo MU-1');
check(str_contains($htmlGoods, 'Maslinovo ulje') &&
    !str_contains($htmlGoods, 'Riječka sol'), 'only published webshop goods in multi-term search');
$htmlBranch = $core->publicCatalogHtml('proizvodi', 'RI-01', 'sol');
check(str_contains($htmlBranch, 'Riječka sol') &&
    !str_contains($htmlBranch, 'Maslinovo ulje'),
    'branch pricing never mixed with webshop');
$htmlServices = $core->publicCatalogHtml('usluge', '', 'materijalu');
check(str_contains($htmlServices, 'Izrada po mjeri') &&
    str_contains($htmlServices, 'Na upit') &&
    !str_contains($htmlServices, 'Dostava prema dogovoru'),
    'inquiry criteria searchable separately without fake numeric prices');
check(!str_contains($htmlServices, 'Servis uređaja'),
    'numeric services obey the same published query');
try {
    $core->publicCatalogHtml('usluge', '../../etc', '');
    throw new RuntimeException('Unsafe public outlet accepted.');
} catch (InvalidArgumentException $ignored) {}
try {
    $core->publicCatalogHtml('usluge', '', str_repeat('Q', 300));
    throw new RuntimeException('Oversized search accepted.');
} catch (InvalidArgumentException $ignored) {}

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
// Public compatibility schema is frozen: documentary source references are
// private even in archives. Android may rely on the original v1 headers.
$expectedPublicFields = [
    'id', 'naziv', 'vrsta', 'sku', 'marka', 'neto_kolicina', 'ean_barkod',
    'jedinica_mjere', 'cijena_jedinice_mjere', 'maloprodajna_cijena',
    'naziv_posebnog_oblika', 'sidrena_cijena', 'sidrena_datum',
    'barkod', 'dostupnost', 'slika_url',
];
check(Core::PUBLIC_FIELDS === $expectedPublicFields &&
    str_getcsv(strtok(substr($csv, 3), "\n"), ';', '"', '') === $expectedPublicFields,
    'Legacy public CSV retains exact Android v1 columns/order');
// Attack-looking names must never become formulas in the public CSV.
$injectedSnapshot = $core->read('published.json');
$injectedSnapshot['products'][0]['naziv'] = '=HYPERLINK("https://example.test", "Click")';
$injectedCsv = $core->csv('proizvodi', '', $injectedSnapshot);
check(str_contains($injectedCsv, "'=HYPERLINK") &&
    !str_contains($injectedCsv, ';=HYPERLINK'),
    'Public compatibility CSV neutralizes merchant-controlled spreadsheet formulas');
check(!str_contains($csv, 'Arhiva cjenika 2026') &&
    !str_contains($csv, 'historical_source') &&
    !str_contains($csv, 'historical_method') &&
    !str_contains($csv, 'reference_scope'),
    'Public compatibility CSV never exposes private historical evidence');
$xml = $core->xml('proizvodi');
check(!str_contains($xml, '<historical_source>') &&
    !str_contains($xml, '<historical_method>') &&
    !str_contains($xml, '<reference_scope>') &&
    !str_contains($xml, 'Arhiva cjenika 2026'),
    'Public XML never exposes private historical source or method');
// A pre-1.5.24 published snapshot may contain an unverified baseline
// which must not be claimed as legally verified after upgrading.
$legacyPublished = $core->read('published.json');
$legacyPublished['products'][0]['historical_verified'] = false;
$legacyPrivate = new Core($root . '/legacy-unverified', 'https://primjer.hr');
$legacyPrivate->write('published.json', $legacyPublished);
$legacyCsv = $legacyPrivate->csv('proizvodi');
$legacyHtml = $legacyPrivate->publicCatalogHtml('proizvodi');
check(str_contains($legacyCsv, 'Maslinovo ulje') &&
    !str_contains($legacyCsv, ';8,49;') &&
    !str_contains($legacyHtml, '8,49 €') &&
    str_contains($legacyHtml, 'Maloprodajna cijena'),
    'Legacy unverified baseline is redacted while current product price remains public');
check(!str_contains($legacyPrivate->xml('proizvodi'), '<sidrena_cijena>8,49</sidrena_cijena>'),
    'Legacy unverified historical price is also absent from public XML');
check(str_contains($xml, '<bazna-cjenik') && str_contains($xml, '<sidrena_cijena>8,49</sidrena_cijena>'),
    'XML and CSV price parity');
$core->saveDraft([
    'outlets' => $catalog['outlets'],
    'products' => [[
        'id' => 'SKU-1', 'naziv' => 'Maslinovo ulje',
        'maloprodajna_cijena' => '6,19', 'sidrena_cijena' => '8,49',
        'sidrena_datum' => '2026-09-10', 'historical_verified' => true, 'historical_source' => 'Arhiva cjenika 2026', 'reference_scope' => 'general_goods_services', 'historical_method' => 'reference_day',
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
check(!str_contains($core->archiveCsv($matches[1], 'proizvodi'), 'Arhiva cjenika 2026') &&
    !str_contains($core->archiveCsv($matches[1], 'proizvodi'), 'historical_source'),
    'Published archives never disclose documentary source identifiers');
check(str_contains($core->archiveCsv($matches[1], 'proizvodi'), '5,99'),
    'archive link remains stable after another publication');
$remove = static function ($dir) use (&$remove): void {
    foreach (scandir($dir) ?: [] as $part) {
        if ($part === '.' || $part === '..') continue;
        $p = $dir . '/' . $part;
        if (is_dir($p)) $remove($p); else unlink($p);
    }
    rmdir($dir);
};
$remove($root);
echo "PASS: BAZNA cross-platform catalog, historical validation, immutable archive, public v1, HTTPS and unrestricted authenticated editing\n";
