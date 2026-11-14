<?php
/**
 * Server-side operator CLI. Never web-accessible. Shopify app installation
 * must be authorized separately via Dev Dashboard and read_products scope.
 *
 * php shopify/bin/manage.php shop.myshopify.com status|sync|publish
 * Shopify installation credentials are required only for authorized remote sync.
 */
declare(strict_types=1);
use Brendigo\Bazna\Core;
use Brendigo\Bazna\Shopify\ProxySignature;
use Brendigo\Bazna\Shopify\WebhookHandler;
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require_once dirname(__DIR__, 2) . '/web/src/Core.php';
require_once dirname(__DIR__) . '/server/ProxySignature.php';
require_once dirname(__DIR__) . '/server/WebhookHandler.php';

try {
    if (count($argv) !== 3) throw new InvalidArgumentException('Usage: php manage.php SHOP status|sync|publish');
    $shop = ProxySignature::shop($argv[1]);
    ProxySignature::requireInstalled($shop, (string) getenv('BAZNA_SHOPIFY_ALLOWED_SHOPS'));
    $storage = (string) getenv('BAZNA_SHOPIFY_PRIVATE_DIR');
    if (!str_starts_with($storage, '/') || str_contains($storage, '..')) {
        throw new RuntimeException('BAZNA_SHOPIFY_PRIVATE_DIR nije konfiguriran.');
    }
    if (WebhookHandler::isRevoked($storage, $shop)) {
        throw new RuntimeException('Instalacija Shopify trgovine je opozvana. Potrebna je nova odobrena instalacija.');
    }
    $core = new Core(rtrim($storage, '/') . '/shop-' . hash('sha256', $shop),
        'https://' . $shop . '/apps');
    $core->ensurePrivate();
    $command = $argv[2];
    if ($command === 'status') {
        echo json_encode(['shop' => $shop,
            'last_publication' => $core->read('published.json')['published_at'] ?? null], JSON_PRETTY_PRINT) . "\n";
    } elseif ($command === 'sync') {
        require_once __DIR__ . '/sync.php';
        $count = bazna_shopify_sync($core, $shop, (string) getenv('BAZNA_SHOPIFY_ACCESS_TOKEN'));
        echo "BAZNA imported $count Shopify variants as UNVERIFIED draft.\n";
    } elseif ($command === 'publish') {
        $published = $core->publish();
        echo 'BAZNA published ' . $published['published_at'] . "\n";
    } else {
        throw new InvalidArgumentException('Nepoznata radnja.');
    }
} catch (Throwable $e) {
    fwrite(STDERR, 'BAZNA Shopify: ' . $e->getMessage() . "\n");
    exit(1);
}
