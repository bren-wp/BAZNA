<?php
/**
 * Dedicated Shopify webhook entry point. Route /shopify/webhooks/* here
 * on the HTTPS BAZNA application host. Never use the app-proxy endpoint for
 * webhooks: Shopify's webhook HMAC is calculated over the raw JSON body.
 */
declare(strict_types=1);

use Brendigo\Bazna\Shopify\WebhookHandler;

require_once __DIR__ . '/ProxySignature.php';
require_once __DIR__ . '/WebhookHandler.php';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, max-age=0, must-revalidate');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');

function bazna_webhook_reply(int $code, array $response): never {
    http_response_code($code);
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR);
    exit;
}

if (($_SERVER['HTTPS'] ?? '') !== 'on') {
    bazna_webhook_reply(403, ['ok' => false, 'code' => 'https_required']);
}
if (strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? '')) !== 'POST') {
    header('Allow: POST');
    bazna_webhook_reply(405, ['ok' => false, 'code' => 'method_not_allowed']);
}
if (!str_starts_with(strtolower((string) ($_SERVER['CONTENT_TYPE'] ?? '')), 'application/json')) {
    bazna_webhook_reply(415, ['ok' => false, 'code' => 'json_required']);
}
$length = (int) ($_SERVER['CONTENT_LENGTH'] ?? -1);
if ($length > WebhookHandler::MAX_BYTES) {
    bazna_webhook_reply(413, ['ok' => false, 'code' => 'too_large']);
}
$body = file_get_contents('php://input', false, null, 0, WebhookHandler::MAX_BYTES + 1);
if (!is_string($body)) {
    bazna_webhook_reply(400, ['ok' => false, 'code' => 'invalid_body']);
}
[$code, $result] = WebhookHandler::process(
    $body,
    [
        'hmac' => (string) ($_SERVER['HTTP_X_SHOPIFY_HMAC_SHA256'] ?? ''),
        'shop' => (string) ($_SERVER['HTTP_X_SHOPIFY_SHOP_DOMAIN'] ?? ''),
        'topic' => (string) ($_SERVER['HTTP_X_SHOPIFY_TOPIC'] ?? ''),
    ],
    (string) getenv('BAZNA_SHOPIFY_CLIENT_SECRET'),
    (string) getenv('BAZNA_SHOPIFY_PRIVATE_DIR')
);
bazna_webhook_reply($code, $result);
