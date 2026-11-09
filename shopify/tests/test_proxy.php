<?php
/** Deterministic offline HMAC, replay and shop-isolation tests. */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require_once dirname(__DIR__) . '/server/ProxySignature.php';
use Brendigo\Bazna\Shopify\ProxySignature;
function checkShop(bool $ok, string $message): void {
    if (!$ok) throw new RuntimeException('BAZNA Shopify HMAC test failed: ' . $message);
}
$secret = str_repeat('a', 40);
$now = 1794301200;
$fields = [
    'shop' => 'trgovina.myshopify.com',
    'path_prefix' => '/apps/bazna',
    'timestamp' => (string) $now,
    'extra' => ['one', 'two'],
];
$parts = [];
foreach ($fields as $key => $values) {
    $parts[] = $key . '=' . implode(',', (array) $values);
}
sort($parts, SORT_STRING);
$signature = hash_hmac('sha256', implode('', $parts), $secret);
$query = 'shop=trgovina.myshopify.com&path_prefix=%2Fapps%2Fbazna&timestamp=' . $now .
    '&extra=one&extra=two&signature=' . $signature;
$parsed = ProxySignature::verify($query, $secret, $now);
checkShop($parsed['shop'] === 'trgovina.myshopify.com', 'signed shop identity');
ProxySignature::requireInstalled($parsed['shop'], 'trgovina.myshopify.com,druga.myshopify.com');
foreach ([
    str_replace('shop=trgovina', 'shop=napadac', $query),
    $query . '&signature=' . $signature,
    str_replace('extra=two', 'extra=three', $query),
] as $forged) {
    try {
        ProxySignature::verify($forged, $secret, $now);
        throw new RuntimeException('Forged proxy was accepted.');
    } catch (InvalidArgumentException $ignored) {}
}
try {
    ProxySignature::verify($query, $secret, $now + 360);
    throw new RuntimeException('Old proxy request was accepted.');
} catch (InvalidArgumentException $ignored) {}
try {
    ProxySignature::requireInstalled('trgovina.myshopify.com', 'druga.myshopify.com');
    throw new RuntimeException('Unknown shop was accepted.');
} catch (InvalidArgumentException $ignored) {}
try {
    ProxySignature::shop('https://trgovina.myshopify.com/admin');
    throw new RuntimeException('Untrusted URL was accepted.');
} catch (InvalidArgumentException $ignored) {}
echo "PASS: BAZNA Shopify signed app-proxy requests, replay window and installed shop isolation\n";
