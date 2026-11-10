<?php
/** Shopify BAZNA privacy webhooks: offline HMAC, tenant isolation and erasure. */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }

require_once dirname(__DIR__) . '/server/ProxySignature.php';
require_once dirname(__DIR__) . '/server/WebhookHandler.php';

use Brendigo\Bazna\Shopify\WebhookHandler;

function expectWebhook(bool $actual, string $why): void {
    if (!$actual) throw new RuntimeException('BAZNA webhook test: ' . $why);
}

$secret = str_repeat('h', 48);
$root = sys_get_temp_dir() . '/bazna-webhook-' . bin2hex(random_bytes(5));
mkdir($root, 0700, true);
$webroot = $root . '/public';
$private = $root . '/private';
mkdir($webroot, 0700);
mkdir($private, 0700);
$_SERVER['DOCUMENT_ROOT'] = $webroot;

$shop = 'trgovina.myshopify.com';
$other = 'druga.myshopify.com';
$tenant = $private . '/' . WebhookHandler::tenantId($shop);
$otherTenant = $private . '/' . WebhookHandler::tenantId($other);
mkdir($tenant, 0700);
mkdir($otherTenant, 0700);
file_put_contents($tenant . '/license.json', '{"encrypted_key":"do-not-disclose"}');
file_put_contents($tenant . '/published.json', '{"private":"shop_data"}');
file_put_contents($otherTenant . '/published.json', '{"private":"keep-other-shop"}');

$send = static function (string $topic, array $data, ?string $sig = null,
    ?string $headerShop = null) use ($secret, $private, $shop): array {
    $raw = json_encode($data, JSON_THROW_ON_ERROR);
    $headers = [
        'hmac' => $sig ?? base64_encode(hash_hmac('sha256', $raw, $secret, true)),
        'topic' => $topic, 'shop' => $headerShop ?? $shop,
    ];
    return WebhookHandler::process($raw, $headers, $secret, $private);
};

[$status, $result] = $send('customers/data_request', ['shop_domain' => $shop]);
expectWebhook($status === 200 && $result['code'] === 'no_customer_data',
    'no customer PII stored');
expectWebhook(is_file($tenant . '/license.json'), 'customer request cannot delete shop');

[$status, $result] = $send('customers/redact', ['shop_domain' => $shop]);
expectWebhook($status === 200 && $result['code'] === 'no_customer_data',
    'customer redaction is idempotent');

[$status, $result] = $send('shop/redact', ['shop_domain' => $shop], 'forged');
expectWebhook($status === 401 && $result['code'] === 'invalid_hmac',
    'forged HMAC is rejected before storage mutations');
expectWebhook(is_file($tenant . '/license.json'), 'forged request did not erase data');

[$status, $result] = $send('shop/redact', ['shop_domain' => $shop], null, $other);
expectWebhook($status === 400 && $result['code'] === 'shop_mismatch',
    'signed body cannot be reassigned to another shop');

[$status, $result] = $send('shop/redact', ['anything' => 'missing shop identity']);
expectWebhook($status === 400 && $result['code'] === 'missing_shop_identity',
    'a destructive signed webhook cannot select the tenant by unsigned header');

[$status, $result] = $send('app/uninstalled', ['myshopify_domain' => $shop]);
expectWebhook($status === 200 && $result['code'] === 'shop_revoked',
    'Shopify uninstall tombstone created');
expectWebhook(WebhookHandler::isRevoked($private, $shop), 'revocation blocks app proxy');
expectWebhook(is_file($tenant . '/published.json'),
    'uninstall keeps data only until later Shopify shop/redact event');
expectWebhook(!WebhookHandler::isRevoked($private, $other),
    'revocation does not affect a different installed store');

[$status, $result] = $send('shop/redact', ['shop_domain' => $shop]);
expectWebhook($status === 200 && $result['code'] === 'shop_erased',
    'verified Shopify privacy deletion');
expectWebhook(!file_exists($tenant) && is_file($otherTenant . '/published.json'),
    'only the correct tenant is erased, including local tokens and archives');
expectWebhook(WebhookHandler::isRevoked($private, $shop), 'erasure remains revoked');

[$status, $result] = $send('shop/redact', ['shop_domain' => $shop]);
expectWebhook($status === 200 && !file_exists($tenant), 'duplicate webhook idempotent');

[$status, $result] = WebhookHandler::process(str_repeat('x', WebhookHandler::MAX_BYTES + 1),
    ['hmac' => '', 'topic' => 'shop/redact', 'shop' => $shop], $secret, $private);
expectWebhook($status === 413, 'oversized body rejected');

$outside = $root . '/outside-protected.txt';
file_put_contents($outside, 'not-a-shop');
mkdir($tenant, 0700);
if (function_exists('symlink') && symlink($outside, $tenant . '/escape')) {
    [$status, $result] = $send('shop/redact', ['shop_domain' => $shop]);
    expectWebhook($status === 503 && file_get_contents($outside) === 'not-a-shop',
        'symlink cannot cause deletion outside a tenant directory');
    unlink($tenant . '/escape');
    rmdir($tenant);
}
function cleanupWebhook(string $dir): void {
    foreach (scandir($dir) ?: [] as $name) {
        if ($name === '.' || $name === '..') continue;
        $file = $dir . '/' . $name;
        if (is_link($file)) unlink($file);
        elseif (is_dir($file)) cleanupWebhook($file);
        else unlink($file);
    }
    rmdir($dir);
}
cleanupWebhook($root);
echo "PASS: Shopify webhook HMAC, mandatory privacy topics, tenant erasure, uninstall revocation, replay and symlink safety\n";
