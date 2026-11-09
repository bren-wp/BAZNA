<?php
/**
 * Server-side operator CLI. Never web-accessible. Shopify app installation
 * must be authorized separately via Dev Dashboard and read_products scope.
 *
 * php shopify/bin/manage.php shop.myshopify.com status|activate|refresh|deactivate|sync|publish
 * Set BAZNA_LICENSE_KEY only for 'activate'; never pass keys as CLI args.
 */
declare(strict_types=1);
use Brendigo\Bazna\Core;
use Brendigo\Bazna\LicenseClient;
use Brendigo\Bazna\Shopify\ProxySignature;
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require_once dirname(__DIR__, 2) . '/web/src/Core.php';
require_once dirname(__DIR__, 2) . '/web/src/LicenseClient.php';
require_once dirname(__DIR__) . '/server/ProxySignature.php';

try {
    if (count($argv) !== 3) throw new InvalidArgumentException('Usage: php manage.php SHOP status|activate|refresh|deactivate|sync|publish');
    $shop = ProxySignature::shop($argv[1]);
    ProxySignature::requireInstalled($shop, (string) getenv('BAZNA_SHOPIFY_ALLOWED_SHOPS'));
    $storage = (string) getenv('BAZNA_SHOPIFY_PRIVATE_DIR');
    if (!str_starts_with($storage, '/') || str_contains($storage, '..')) {
        throw new RuntimeException('BAZNA_SHOPIFY_PRIVATE_DIR nije konfiguriran.');
    }
    $core = new Core(rtrim($storage, '/') . '/shop-' . hash('sha256', $shop),
        'https://' . $shop . '/apps');
    $core->ensurePrivate();
    $secret = (string) getenv('BAZNA_SHOPIFY_APP_KEY');
    $license = new LicenseClient($core, $secret);
    $command = $argv[2];
    if ($command === 'status') {
        echo json_encode(['shop' => $shop, 'license_active' => $license->active(),
            'last_publication' => $core->read('published.json')['published_at'] ?? null], JSON_PRETTY_PRINT) . "\n";
    } elseif ($command === 'activate') {
        $key = (string) getenv('BAZNA_LICENSE_KEY');
        $reply = $license->activate($key);
        echo json_encode($reply) . "\n";
        if (empty($reply['ok'])) exit(2);
    } elseif ($command === 'refresh') {
        $reply = $license->refresh();
        echo json_encode($reply) . "\n";
        if (empty($reply['ok'])) exit(2);
    } elseif ($command === 'deactivate') {
        $reply = $license->deactivate();
        echo json_encode($reply) . "\n";
        if (empty($reply['ok'])) exit(2);
    } elseif ($command === 'sync') {
        if (!$license->active()) throw new RuntimeException('Za sinkronizaciju je potrebna aktivna licenca.');
        require_once __DIR__ . '/sync.php';
        $count = bazna_shopify_sync($core, $shop, (string) getenv('BAZNA_SHOPIFY_ACCESS_TOKEN'));
        echo "BAZNA imported $count Shopify variants as UNVERIFIED draft.\n";
    } elseif ($command === 'publish') {
        if (!$license->active()) throw new RuntimeException('Za objavu je potrebna aktivna licenca.');
        $published = $core->publish();
        echo 'BAZNA published ' . $published['published_at'] . "\n";
    } else {
        throw new InvalidArgumentException('Nepoznata radnja.');
    }
} catch (Throwable $e) {
    fwrite(STDERR, 'BAZNA Shopify: ' . $e->getMessage() . "\n");
    exit(1);
}
