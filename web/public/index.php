<?php
/**
 * BAZNA standalone PHP entry point.
 * Point the HTTPS virtual host DocumentRoot to web/public and set environment
 * variables per web/README.md. NEVER serve web/src or the JSON storage.
 */
declare(strict_types=1);
use Brendigo\Bazna\Core;
use Brendigo\Bazna\AdminCatalog;

require_once dirname(__DIR__) . '/src/Core.php';
require_once dirname(__DIR__) . '/src/AdminCatalog.php';

header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');
header('X-Frame-Options: DENY');
header('Cache-Control: no-store, max-age=0, must-revalidate');

function bazna_json(int $status, mixed $value): never {
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($value, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}
function bazna_fail(int $status, string $message): never {
    bazna_json($status, ['error' => $message]);
}
function bazna_html(string $s): string { return Core::esc($s); }

try {
    if ((($_SERVER['HTTPS'] ?? '') !== 'on') && PHP_SAPI !== 'cli-server') {
        bazna_fail(403, 'BAZNA zahtijeva HTTPS.');
    }
    $storage = (string) getenv('BAZNA_PRIVATE_DIR');
    $publicUrl = (string) getenv('BAZNA_PUBLIC_URL');
    $core = new Core($storage, $publicUrl);
    $core->ensurePrivate();
    $prefix = rtrim((string) parse_url($publicUrl, PHP_URL_PATH), '/');
    $path = rawurldecode((string) parse_url((string) ($_SERVER['REQUEST_URI'] ?? '/'), PHP_URL_PATH));
    if ($prefix !== '' && $prefix !== '/' && str_starts_with($path, $prefix . '/')) {
        $path = substr($path, strlen($prefix));
    }
    if ($path === '/' || $path === '') $path = '/bazna/cjenik';
    if (str_contains($path, "\0") || str_contains($path, '..')) bazna_fail(400, 'Neispravna putanja.');

    // This module is embedded by the Joomla adapter as well as standalone.
    $method = strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET'));
    if ($method === 'GET' && $path === '/bazna/v1/cjenici') {
        bazna_json(200, $core->discovery('web'));
    }
    if ($method === 'GET' && $path === '/bazna/v1/usluge-na-upit') {
        bazna_json(200, $core->onRequestServices(
            isset($_GET['outlet']) && is_string($_GET['outlet']) ? $_GET['outlet'] : '',
            isset($_GET['q']) && is_string($_GET['q']) ? $_GET['q'] : ''
        ));
    }
    if ($method === 'GET' && $path === '/bazna/v1/arhiva') {
        $entries = $core->archives();
        $kind = (string) ($_GET['kind'] ?? '');
        if ($kind !== '' && !in_array($kind, Core::KINDS, true)) bazna_fail(400, 'Neispravna vrsta.');
        $outlet = (string) ($_GET['outlet'] ?? '');
        bazna_json(200, array_values(array_filter($entries, static function ($a) use ($kind, $outlet): bool {
            return ($kind === '' || $a['kind'] === $kind) &&
                ($outlet === '' || $a['outlet'] === $outlet);
        })));
    }
    if ($method === 'GET' && preg_match('#^/bazna/v1/cjenici/(proizvodi|usluge)/(csv|xml)$#D', $path, $parts)) {
        $outlet = (string) ($_GET['oznaka'] ?? '');
        $csv = $core->csv($parts[1], $outlet);
        if ($parts[2] === 'csv') {
            header('Content-Type: text/csv; charset=utf-8');
            echo $csv; exit;
        }
        header('Content-Type: application/xml; charset=utf-8');
        echo $core->xml($parts[1], $outlet); exit;
    }
    if ($method === 'GET' && preg_match('#^/bazna/v1/arhiva/([0-9]{14}-[a-f0-9]{12})/(proizvodi|usluge)/csv$#D', $path, $parts)) {
        header('Content-Type: text/csv; charset=utf-8');
        echo $core->archiveCsv($parts[1], $parts[2], (string) ($_GET['oznaka'] ?? '')); exit;
    }
    if ($method === 'GET' && $path === '/bazna/assets/bazna.css') {
        header('Content-Type: text/css; charset=utf-8');
        readfile(__DIR__ . '/assets/bazna.css'); exit;
    }

    $hash = (string) getenv('BAZNA_ADMIN_PASSWORD_HASH');
    if ($path === '/bazna/admin') {
        if ($hash === '') bazna_fail(503, 'Administratorska lozinka nije konfigurirana.');
        if ($method !== 'GET' && $method !== 'POST') bazna_fail(405, 'Nedopuštena metoda.');
        ini_set('session.use_strict_mode', '1');
        session_name('bazna_session');
        session_set_cookie_params([
            'httponly' => true, 'secure' => PHP_SAPI !== 'cli-server',
            'samesite' => 'Strict', 'path' => $core->basePath(),
        ]);
        session_start();
        if (!isset($_SESSION['csrf'])) $_SESSION['csrf'] = bin2hex(random_bytes(32));
        $message = '';
        if ($method === 'POST') {
            $formToken = (string) ($_POST['_csrf'] ?? '');
            if (!hash_equals($_SESSION['csrf'], $formToken)) bazna_fail(403, 'Neispravan CSRF token.');
            $action = (string) ($_POST['action'] ?? '');
            if ($action === 'login') {
                $count = (int) ($_SESSION['failed_attempts'] ?? 0);
                if ($count >= 8) bazna_fail(429, 'Previše pokušaja. Pokušajte u novoj sesiji.');
                if (!password_verify((string) ($_POST['password'] ?? ''), $hash)) {
                    $_SESSION['failed_attempts'] = $count + 1;
                    $message = 'Neispravna lozinka.';
                } else {
                    session_regenerate_id(true);
                    $_SESSION['admin'] = true;
                    $_SESSION['failed_attempts'] = 0;
                    $message = 'Prijava uspješna.';
                }
            } elseif ($action === 'logout' && ($_SESSION['admin'] ?? false)) {
                $_SESSION = [];
                session_regenerate_id(true);
                $message = 'Odjavljeni ste.';
            } elseif (($_SESSION['admin'] ?? false) &&
                in_array($action, ['save', 'publish', 'save_item', 'delete_item'], true)) {
                if ($action === 'save') {
                    $raw = (string) ($_POST['catalog'] ?? '');
                    if (strlen($raw) > 6 * 1024 * 1024) bazna_fail(413, 'Katalog je prevelik.');
                    $payload = json_decode($raw, true, 40, JSON_THROW_ON_ERROR);
                    if (!is_array($payload)) bazna_fail(400, 'Neispravan JSON katalog.');
                    $core->saveDraft($payload);
                    $message = 'Nacrt kataloga je spremljen.';
                } elseif ($action === 'publish') {
                    $core->publish();
                    $message = 'Javni cjenici i arhiva su ažurirani.';
                } else {
                    $collection = (string) ($_POST['collection'] ?? '');
                    $index = AdminCatalog::indexFromPost($_POST);
                    $expected = (string) ($_POST['expected_hash'] ?? '');
                    if ($action === 'delete_item' &&
                        (string) ($_POST['delete_confirm'] ?? '') !== '1') {
                        bazna_fail(400, 'Potvrdite uklanjanje stavke.');
                    }
                    $row = $action === 'save_item' ? AdminCatalog::fromPost($_POST) : null;
                    $core->modifyRow($collection, $index, $row, $expected);
                    $message = $action === 'save_item' ? 'Stavka je spremljena u nacrt.' :
                        'Stavka je uklonjena iz nacrta.';
                }
            }
        }
        include __DIR__ . '/admin.php'; exit;
    }
    if ($method === 'GET' && in_array($path, ['/bazna/cjenik', '/bazna/arhiva'], true)) {
        $records = $core->read('published.json');
        include __DIR__ . '/public-page.php'; exit;
    }
    bazna_fail(404, 'BAZNA stranica nije pronađena.');
} catch (InvalidArgumentException|\JsonException $exception) {
    bazna_fail(400, 'Neispravni podaci: ' . $exception->getMessage());
} catch (Throwable $exception) {
    error_log('BAZNA PHP: ' . $exception->getMessage());
    bazna_fail(503, 'BAZNA nije dostupna. Provjerite konfiguraciju i poslužiteljski dnevnik.');
}
