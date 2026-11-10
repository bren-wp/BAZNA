<?php
/**
 * Shopify privacy and uninstall webhooks for BAZNA.
 *
 * This handler is deliberately separate from the storefront app proxy:
 * webhook HMAC = BASE64(HMAC-SHA256(raw HTTP BODY, client secret)).
 * App-proxy HMAC signs QUERY PARAMETERS and cannot authenticate webhooks.
 *
 * Does not collect any customer PII. On Shopify's shop/redact request,
 * deletes the whole tenant catalogue and locally held secrets, rather than
 * keeping identifiable shop data in a backup reachable from the service.
 */
declare(strict_types=1);

namespace Brendigo\Bazna\Shopify;

use InvalidArgumentException;
use RuntimeException;

final class WebhookHandler
{
    public const MAX_BYTES = 1048576;
    private const TOPICS = ['customers/data_request', 'customers/redact', 'shop/redact', 'app/uninstalled'];

    public static function tenantId(string $shop): string
    {
        return 'shop-' . hash('sha256', ProxySignature::shop($shop));
    }

    private static function root(string $path): string
    {
        if ($path === '' || !str_starts_with($path, '/') || str_contains($path, '..')) {
            throw new RuntimeException('Missing or unsafe private Shopify storage');
        }
        $public = (string) ($_SERVER['DOCUMENT_ROOT'] ?? '');
        if (!is_dir($path) && !mkdir($path, 0700, true) && !is_dir($path)) {
            throw new RuntimeException('Cannot access private Shopify storage');
        }
        $real = realpath($path);
        $webroot = $public === '' ? false : realpath($public);
        if ($real === false || ($webroot !== false &&
            ($real === $webroot || str_starts_with($real, rtrim($webroot, '/') . '/')))) {
            throw new RuntimeException('Shopify storage must be outside document root');
        }
        return $real;
    }

    public static function isRevoked(string $storage, string $shop): bool
    {
        if (!is_dir($storage)) return false;
        $root = self::root($storage);
        return is_file($root . '/revoked-' . self::tenantId($shop) . '.json');
    }

    private static function removeTree(string $path, string $root): void
    {
        // Explicit confinement and no traversal of symlinks/linked dirs.
        if ($path === $root || !str_starts_with($path, $root . '/')) {
            throw new RuntimeException('Invalid tenant deletion boundary');
        }
        if (is_link($path)) {
            throw new RuntimeException('Refusing to follow a tenant symlink');
        }
        if (!is_dir($path)) {
            if (is_file($path) && !unlink($path)) throw new RuntimeException('Cannot remove private file');
            return;
        }
        foreach (scandir($path) ?: [] as $name) {
            if ($name === '.' || $name === '..') continue;
            self::removeTree($path . '/' . $name, $root);
        }
        if (!rmdir($path)) throw new RuntimeException('Cannot remove tenant directory');
    }

    private static function revoke(string $root, string $shop, bool $erase): void
    {
        $tenant = self::tenantId($shop);
        $lockPath = $root . '/shopify-webhook.lock';
        $handle = fopen($lockPath, 'c');
        if ($handle === false || !flock($handle, LOCK_EX)) {
            throw new RuntimeException('Cannot lock Shopify privacy operations');
        }
        try {
            // The tombstone is intentionally *only* a SHA256-derived ID,
            // never the shop domain, a token or customer data.
            $name = $root . '/revoked-' . $tenant . '.json';
            $tmp = tempnam($root, '.revoked-');
            if ($tmp === false) throw new RuntimeException('Cannot save revocation');
            try {
                $payload = json_encode([
                    'revoked_at' => gmdate('c'),
                    'deleted' => $erase,
                ], JSON_THROW_ON_ERROR);
                if (file_put_contents($tmp, $payload, LOCK_EX) !== strlen($payload)) {
                    throw new RuntimeException('Cannot write revocation');
                }
                chmod($tmp, 0600);
                if (!rename($tmp, $name)) throw new RuntimeException('Cannot record revocation');
            } finally {
                if (file_exists($tmp)) unlink($tmp);
            }
            if ($erase) self::removeTree($root . '/' . $tenant, $root);
        } finally {
            flock($handle, LOCK_UN);
            fclose($handle);
        }
    }

    /**
     * Returns HTTP status and simple public response; no Shopify token,
     * customer details or license keys appear in any response.
     */
    public static function process(
        string $raw,
        array $headers,
        string $secret,
        string $storage
    ): array {
        if (strlen($secret) < 32) return [503, ['ok' => false, 'code' => 'not_configured']];
        if (strlen($raw) > self::MAX_BYTES) return [413, ['ok' => false, 'code' => 'too_large']];
        $signature = trim((string) ($headers['hmac'] ?? ''));
        $decoded = base64_decode($signature, true);
        $expected = hash_hmac('sha256', $raw, $secret, true);
        if (!is_string($decoded) || strlen($decoded) !== 32 || !hash_equals($expected, $decoded)) {
            return [401, ['ok' => false, 'code' => 'invalid_hmac']];
        }
        $topic = strtolower(trim((string) ($headers['topic'] ?? '')));
        if (!in_array($topic, self::TOPICS, true)) {
            return [400, ['ok' => false, 'code' => 'unsupported_topic']];
        }
        try {
            $shop = ProxySignature::shop((string) ($headers['shop'] ?? ''));
        } catch (InvalidArgumentException $e) {
            return [400, ['ok' => false, 'code' => 'invalid_shop']];
        }
        $data = json_decode($raw, true);
        if (!is_array($data) || array_is_list($data)) {
            return [400, ['ok' => false, 'code' => 'invalid_json']];
        }
        // A signed BODY may be replayed with a different unsigned shop header.
        // Destructive actions require a body shop domain that matches it.
        $bodyDomain = $data['shop_domain'] ?? $data['myshopify_domain'] ?? null;
        if ($bodyDomain !== null) {
            if (!is_string($bodyDomain) || strtolower($bodyDomain) !== $shop) {
                return [400, ['ok' => false, 'code' => 'shop_mismatch']];
            }
        } elseif (in_array($topic, ['shop/redact', 'app/uninstalled'], true)) {
            return [400, ['ok' => false, 'code' => 'missing_shop_identity']];
        }

        if (in_array($topic, ['customers/data_request', 'customers/redact'], true)) {
            // BAZNA deliberately stores no Shopify customer names, emails,
            // purchase history, or customer IDs. Nothing to export or redact.
            return [200, ['ok' => true, 'code' => 'no_customer_data']];
        }
        try {
            $root = self::root($storage);
            self::revoke($root, $shop, $topic === 'shop/redact');
        } catch (RuntimeException $e) {
            error_log('BAZNA Shopify privacy operation failed');
            return [503, ['ok' => false, 'code' => 'storage_unavailable']];
        }
        return [200, ['ok' => true, 'code' =>
            $topic === 'shop/redact' ? 'shop_erased' : 'shop_revoked']];
    }
}
