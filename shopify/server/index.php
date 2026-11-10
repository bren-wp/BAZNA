<?php
/**
 * BAZNA Shopify app-proxy PHP backend, protected by Shopify HMAC.
 * Configure Shopify Dev Dashboard app proxy:
 * storefront /apps/bazna -> https://YOUR-APP-HOST/shopify/proxy
 * Deployment must route the latter path to this script via the webserver.
 */
declare(strict_types=1);
use Brendigo\Bazna\Core;
use Brendigo\Bazna\LicenseClient;
use Brendigo\Bazna\Shopify\ProxySignature;
use Brendigo\Bazna\Shopify\WebhookHandler;

require_once dirname(__DIR__, 2) . '/web/src/Core.php';
require_once dirname(__DIR__, 2) . '/web/src/LicenseClient.php';
require_once __DIR__ . '/ProxySignature.php';
require_once __DIR__ . '/WebhookHandler.php';
header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store, max-age=0, must-revalidate');
header('Referrer-Policy: no-referrer');

function bazna_shop_response(int $code, array $json): never {
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($json, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR);
    exit;
}
try {
    if (($_SERVER['HTTPS'] ?? '') !== 'on') {
        bazna_shop_response(403, ['error' => 'https_required']);
    }
    if (strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? '')) !== 'GET') {
        bazna_shop_response(405, ['error' => 'read_only']);
    }
    $signed = ProxySignature::verify(
        (string) ($_SERVER['QUERY_STRING'] ?? ''),
        (string) getenv('BAZNA_SHOPIFY_CLIENT_SECRET'),
        time(),
    );
    $shop = $signed['shop'];
    ProxySignature::requireInstalled($shop, (string) getenv('BAZNA_SHOPIFY_ALLOWED_SHOPS'));
    $folder = (string) getenv('BAZNA_SHOPIFY_PRIVATE_DIR');
    if (!str_starts_with($folder, '/') || str_contains($folder, '..')) {
        throw new RuntimeException('Privatni direktorij Shopify podataka nije konfiguriran.');
    }
    // App uninstall and shop redaction tombstones override even a stale
    // operator allowlist. Installation again requires an explicit owner action.
    if (WebhookHandler::isRevoked($folder, $shop)) {
        bazna_shop_response(403, ['error' => 'shop_installation_revoked']);
    }
    // No user-provided directory name: derive a fixed per-tenant identifier
    // from the validated, signed and locally authorized shop identity.
    $core = new Core(rtrim($folder, '/') . '/shop-' . hash('sha256', $shop), 'https://' . $shop . '/apps');
    $core->ensurePrivate();
    $secret = (string) getenv('BAZNA_SHOPIFY_APP_KEY');
    $license = preg_match('/^[a-f0-9]{64}$/i', $secret) ? new LicenseClient($core, $secret) : null;
    $prefix = (string) (getenv('BAZNA_SHOPIFY_BACKEND_PATH') ?: '/shopify/proxy');
    $path = rawurldecode((string) parse_url((string) ($_SERVER['REQUEST_URI'] ?? ''), PHP_URL_PATH));
    if (!str_starts_with($path, rtrim($prefix, '/') . '/')) {
        bazna_shop_response(404, ['error' => 'unrecognized_route']);
    }
    $tail = substr($path, strlen(rtrim($prefix, '/')));
    if ($tail === '/v1/cjenici') {
        bazna_shop_response(200, $core->discovery('shopify', null, $license?->active()));
    }
    if ($tail === '/v1/arhiva') {
        bazna_shop_response(200, $core->archives());
    }
    if ($tail === '/v1/license-proof') {
        $challenge = $license?->proof();
        if ($challenge === null) bazna_shop_response(404, ['error' => 'proof_not_ready']);
        bazna_shop_response(200, ['challenge' => $challenge]);
    }
    if (preg_match('#^/v1/cjenici/(proizvodi|usluge)/(csv|xml)$#D', $tail, $match)) {
        $outlet = (string) ($_GET['oznaka'] ?? '');
        header('Content-Type: ' . ($match[2] === 'csv' ? 'text/csv' : 'application/xml') . '; charset=utf-8');
        echo $match[2] === 'csv' ? $core->csv($match[1], $outlet) : $core->xml($match[1], $outlet);
        exit;
    }
    if (preg_match('#^/v1/arhiva/([0-9]{14}-[a-f0-9]{12})/(proizvodi|usluge)/csv$#D', $tail, $match)) {
        header('Content-Type: text/csv; charset=utf-8');
        echo $core->archiveCsv($match[1], $match[2], (string) ($_GET['oznaka'] ?? '')); exit;
    }
    if ($tail === '/cjenik') {
        header('Content-Type: text/html; charset=utf-8');
        header("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'");
        $catalog = $core->read('published.json');
        echo '<!doctype html><html lang="hr"><meta name="viewport" content="width=device-width, initial-scale=1">';
        echo '<meta charset="utf-8"><title>BAZNA — Shopify</title>';
        echo '<body style="background:#081525;color:#fff;font:16px system-ui;padding:20px">';
        echo '<main style="max-width:900px;margin:auto"><h1>BAZNA — Bazne cijene</h1>';
        echo '<p>Objavljeno: ' . Core::esc((string) ($catalog['published_at'] ?? 'nije objavljeno')) . '</p>';
        foreach (Core::KINDS as $kind) {
            echo '<section style="background:#102237;padding:20px;margin:12px 0;border-radius:12px"><h2>' .
                Core::esc($kind) . '</h2><p><a style="color:#ffb860" href="' .
                Core::esc($core->link('bazna/v1/cjenici/' . $kind . '/csv')) . '">CSV cjenik</a></p></section>';
        }
        echo '</main></body></html>';
        exit;
    }
    bazna_shop_response(404, ['error' => 'not_found']);
} catch (InvalidArgumentException $e) {
    bazna_shop_response(403, ['error' => 'invalid_or_untrusted_shopify_request']);
} catch (Throwable $e) {
    error_log('BAZNA Shopify proxy: ' . $e->getMessage());
    bazna_shop_response(503, ['error' => 'bazna_service_unavailable']);
}
