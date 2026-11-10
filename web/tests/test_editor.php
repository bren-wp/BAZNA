<?php
/**
 * Shared Web + Joomla catalog editor regression and accessibility tests.
 * No Internet access, WordPress runtime, Shopify secrets or real user data.
 */
declare(strict_types=1);

require_once dirname(__DIR__) . '/src/Core.php';
require_once dirname(__DIR__) . '/src/AdminCatalog.php';

use Brendigo\Bazna\Core;
use Brendigo\Bazna\AdminCatalog;

function verifyEditor(bool $assertion, string $message): void {
    if (!$assertion) throw new RuntimeException('BAZNA editor test failed: ' . $message);
}
function rowHash(array $row): string {
    return hash('sha256', json_encode(
        $row, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR
    ));
}
$root = sys_get_temp_dir() . '/bazna-editor-' . bin2hex(random_bytes(7));
mkdir($root . '/webroot', 0700, true);
$_SERVER['DOCUMENT_ROOT'] = $root . '/webroot';
$core = new Core($root . '/private', 'https://primjer.hr');
$core->saveDraft([
    'products' => [[
        'id' => 'sku-a', 'naziv' => 'Baza <script>alert(1)</script>',
        'maloprodajna_cijena' => '12,49',
        'sidrena_cijena' => '11,99', 'sidrena_datum' => '2026-09-10',
        'historical_verified' => true, 'outlet' => 'RI-01',
    ]],
    'services' => [[
        'id' => 'svc-a', 'naziv' => 'Usluga', 'maloprodajna_cijena' => '4,99',
    ]],
    'outlets' => [['code' => 'RI-01', 'name' => 'Rijeka', 'address' => 'Korzo 12']],
]);
$original = $core->read('draft.json');
verifyEditor(count($original['products']) === 1, 'existing product retained');
$render = AdminCatalog::render(
    'proizvodi', $original, 'https://primjer.hr/bazna/admin?tab=proizvodi',
    '<input name="_csrf" type="hidden" value="test-token">', true
);
verifyEditor(str_contains($render, 'Bazna cijena') &&
    str_contains($render, 'maloprodajna_cijena') &&
    str_contains($render, 'Spremi izmjene') &&
    str_contains($render, 'Potvrđujem') &&
    str_contains($render, 'test-token'), 'real edit form, CSRF, legal checkbox');
verifyEditor(!str_contains($render, '<script>alert(1)</script>') &&
    str_contains($render, '&lt;script&gt;'),
    'stored text escaped from HTML injection');

foreach (['usluge', 'poslovnice'] as $tab) {
    $html = AdminCatalog::render($tab, $original, '?bazna=dashboard&tab='.$tab,
        '<input name="csrf" value="joomla">', true);
    verifyEditor(str_contains($html, 'Spremi izmjene'), 'same UI reused on '.$tab);
}
$fields = AdminCatalog::fromPost([
    'row' => [
        'naziv' => '  Novi proizvod  ', 'maloprodajna_cijena' => '20,10',
        'historical_verified' => '0',
    ],
]);
verifyEditor($fields['naziv'] === 'Novi proizvod' &&
    $fields['historical_verified'] === false, 'POST typed and sanitized');
verifyEditor(AdminCatalog::indexFromPost(['index' => '0']) === 0, 'edit row index');
try {
    AdminCatalog::fromPost(['row' => ['naziv' => ['nested']]]); // HTTP parameter pollution
    throw new RuntimeException('Nested input was accepted.');
} catch (InvalidArgumentException $ignored) {}
try {
    AdminCatalog::indexFromPost(['index' => '../../etc/passwd']);
    throw new RuntimeException('Unsafe row index was accepted.');
} catch (InvalidArgumentException $ignored) {}

$changed = $original['products'][0];
$changed['maloprodajna_cijena'] = '12,59';
$core->modifyRow('products', 0, $changed, rowHash($original['products'][0]));
verifyEditor($core->read('draft.json')['products'][0]['maloprodajna_cijena'] === '12,59',
    'atomic existing row edit');
try {
    $core->modifyRow('products', 0, $changed, rowHash($original['products'][0]));
    throw new RuntimeException('Stale edit overwrote newer data.');
} catch (InvalidArgumentException $ignored) {}
$newProduct = [
    'naziv' => 'Novi artikl', 'maloprodajna_cijena' => '2,49',
    'outlet' => 'RI-01',
];
$core->modifyRow('products', -1, $newProduct);
$updated = $core->read('draft.json');
verifyEditor(count($updated['products']) === 2 &&
    str_starts_with($updated['products'][1]['id'], 'bazna-'),
    'added product with automatically generated stable ID');
try {
    $core->modifyRow('products', 1, array_merge($updated['products'][1],
        ['id' => 'sku-a']), rowHash($updated['products'][1]));
    throw new RuntimeException('Existing product ID changed.');
} catch (InvalidArgumentException $ignored) {}
try {
    $core->modifyRow('outlets', 0, null, rowHash($updated['outlets'][0]));
    throw new RuntimeException('Outlet deletion orphaned published product.');
} catch (InvalidArgumentException $ignored) {}
$core->modifyRow('products', 1, null, rowHash($updated['products'][1]));
verifyEditor(count($core->read('draft.json')['products']) === 1, 'delete only selected row');
$core->publish();
verifyEditor(str_contains($core->csv('proizvodi', 'RI-01'), '12,59'),
    'edited branch catalog is published with actual RI-01 price');
verifyEditor(!str_contains($core->csv('proizvodi'), '12,59'),
    'branch product is not mixed into webshop-only publication');
verifyEditor(!str_contains($core->csv('proizvodi', 'RI-01'), 'Novi artikl'),
    'deleted row does not leak into published CSV');
$core->modifyRow('services', -1, ['naziv'=>'Nova usluga','maloprodajna_cijena'=>'50,00']);
$core->publish();
$history = $core->archives();
verifyEditor(count($history) >= 4, 'published audit history retained with outlets');

function removeEditorTest(string $folder): void {
    foreach (scandir($folder) ?: [] as $entry) {
        if ($entry === '.' || $entry === '..') continue;
        $path = $folder . '/' . $entry;
        if (is_dir($path)) removeEditorTest($path);
        else unlink($path);
    }
    rmdir($folder);
}
removeEditorTest($root);
echo "PASS: BAZNA Web/Joomla shared UI, CSRF markup, XSS escaping, typed inputs, atomic CRUD and stable archives\n";
