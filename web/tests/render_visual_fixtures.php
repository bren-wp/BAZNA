<?php
/**
 * CI-only visual QA fixtures from ACTUAL BAZNA PHP templates + shared Joomla
 * editor. No static hand-drawn screenshot mocks or generated artwork.
 *
 * Usage: php web/tests/render_visual_fixtures.php OUTPUT_DIRECTORY
 */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require_once dirname(__DIR__) . '/src/Core.php';
require_once dirname(__DIR__) . '/src/AdminCatalog.php';

use Brendigo\Bazna\Core;
use Brendigo\Bazna\AdminCatalog;

// admin.php is normally included from index.php, which supplies this escaped
// text helper. Reuse the real helper in the isolated PHP-CLI rendering test.
function bazna_html(string $value): string { return Core::esc($value); }

$output = (string) ($argv[1] ?? '');
if ($output === '' || str_contains($output, '..')) throw new RuntimeException('Safe output directory required.');
if (!is_dir($output) && !mkdir($output, 0700, true)) throw new RuntimeException('Cannot create QA output');
$output = realpath($output);
if (!$output) throw new RuntimeException('Invalid QA output directory');

$private = sys_get_temp_dir() . '/bazna-visual-fixtures-' . bin2hex(random_bytes(6));
$_SERVER['DOCUMENT_ROOT'] = dirname(__DIR__) . '/public';
$core = new Core($private, 'https://primjer.hr');
$catalog = [
    'outlets' => [
        ['name' => 'Rijeka — glavni prodajni objekt', 'code' => 'RI-01', 'address' => 'Korzo 12, Rijeka'],
        ['name' => 'Split — poslovnica', 'code' => 'ST-02', 'address' => 'Riva 1, Split'],
    ],
    'products' => [
        [
            'id' => 'pro-001', 'naziv' => 'Maslinovo ulje ekstra djevičansko premium',
            'maloprodajna_cijena' => '8,49', 'sidrena_cijena' => '7,99',
            'sidrena_datum' => '2026-09-10', 'historical_verified' => true,
            'sku' => 'MU-500', 'marka' => 'BAZNA primjer',
            'jedinica_mjere' => 'L', 'outlet' => '',
        ],
        [
            'id' => 'pro-002', 'naziv' => 'Dugačak naziv testnog proizvoda koji provjerava prelijevanje kartica na vrlo uskim zaslonima i na većim monitorima',
            'maloprodajna_cijena' => '12,99', 'sidrena_cijena' => '',
            'outlet' => 'RI-01',
        ],
    ],
    'services' => [
        [
            'id' => 'svc-001', 'naziv' => 'Servis i održavanje',
            'maloprodajna_cijena' => '49,90', 'sidrena_cijena' => '45,00',
            'sidrena_datum' => '2026-09-10', 'historical_verified' => true,
            'outlet' => '',
        ],
    ],
];
$core->saveDraft($catalog);
$core->publish();
$stylesheet = 'file://' . realpath(dirname(__DIR__) . '/public/assets/bazna.css');
$remoteCss = $core->link('bazna/assets/bazna.css');
$adminTabs = [
    'pregled','proizvodi','usluge','poslovnice','uvoz-izvoz',
    'javni-cjenici','arhiva','postavke','licenca',
];
foreach ($adminTabs as $tab) {
    $_GET = ['tab' => $tab, 'page' => '1'];
    $_SESSION = ['admin' => true, 'csrf' => 'ci-demo-token'];
    $license = null;
    $message = '';
    ob_start();
    include dirname(__DIR__) . '/public/admin.php';
    $html = ob_get_clean();
    $html = str_replace($remoteCss, $stylesheet, $html);
    file_put_contents($output . '/web-' . $tab . '.html', $html);
}
$records = $core->read('published.json');
foreach (['cjenik', 'arhiva'] as $view) {
    $path = '/bazna/' . $view;
    $_GET = ['kind' => 'proizvodi'];
    ob_start();
    include dirname(__DIR__) . '/public/public-page.php';
    $html = ob_get_clean();
    file_put_contents($output . '/web-public-' . $view . '.html',
        str_replace($remoteCss, $stylesheet, $html));
}
// Joomla ADMIN page uses this exact AdminCatalog renderer and CSS file.
// Only the Joomla CMS outer shell is a controlled stand-in here; Joomla
// plugin installation/runtime still needs separate real-CMS verification.
foreach (['proizvodi', 'usluge', 'poslovnice'] as $tab) {
    $nav = '';
    foreach ($adminTabs as $item) {
        $nav .= '<a href="#"' . ($tab === $item ? ' aria-current="page"' : '') .
            '>' . Core::esc(ucfirst(str_replace('-', ' ', $item))) . '</a>';
    }
    $fragment = AdminCatalog::render($tab, $core->read('draft.json'),
        '?bazna=dashboard&tab='.$tab, '<input name="ci-token" value="qa">', true);
    $html = '<!doctype html><html lang="hr"><head><meta charset="utf-8">' .
        '<meta name="viewport" content="width=device-width,initial-scale=1">' .
        '<title>BAZNA Joomla — ' . Core::esc($tab) . '</title>' .
        '<link rel="stylesheet" href="' . htmlspecialchars($stylesheet, ENT_QUOTES) . '"></head>' .
        '<body class="bazna-admin"><header class="bazna-topbar"><strong>BAZNA · Joomla</strong></header>' .
        '<div class="bazna-workspace"><nav class="bazna-sidebar">' . $nav . '</nav>' .
        '<main class="bazna-main"><h1>' . Core::esc(ucfirst($tab)) .
        '</h1>' . $fragment . '</main></div></body></html>';
    file_put_contents($output . '/joomla-' . $tab . '.html', $html);
}
function clearVisualFixture(string $dir): void {
    foreach (scandir($dir) ?: [] as $file) {
        if ($file === '.' || $file === '..') continue;
        $p = $dir . '/' . $file;
        if (is_dir($p)) clearVisualFixture($p);
        else unlink($p);
    }
    rmdir($dir);
}
clearVisualFixture($private);
echo "PASS: 14 actual BAZNA web/joomla-shared UI fixtures rendered without sensitive values\n";
