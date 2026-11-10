<?php
/** Shopify app-proxy HMAC and installed-store verification. */
declare(strict_types=1);
namespace Brendigo\Bazna\Shopify;
use InvalidArgumentException;

final class ProxySignature
{
    public static function shop(string $value): string
    {
        $name = strtolower(trim($value));
        if (!preg_match('/^[a-z0-9](?:[a-z0-9-]{0,60}[a-z0-9])?\.myshopify\.com$/D', $name)) {
            throw new InvalidArgumentException('Nevaljana Shopify trgovina.');
        }
        return $name;
    }

    public static function verify(string $query, string $secret, int $now): array
    {
        if (strlen($query) > 8192 || strlen($secret) < 32) {
            throw new InvalidArgumentException('Neispravna konfiguracija Shopify zahtjeva.');
        }
        $fields = [];
        $signature = null;
        foreach (explode('&', $query) as $pair) {
            if ($pair === '') continue;
            [$rawKey, $rawValue] = array_pad(explode('=', $pair, 2), 2, '');
            $key = urldecode($rawKey);
            $value = urldecode($rawValue);
            if ($key === '' || strlen($key) > 100 || strlen($value) > 2048) {
                throw new InvalidArgumentException('Neispravan parametar.');
            }
            if ($key === 'signature') {
                if ($signature !== null) throw new InvalidArgumentException('Dvostruki potpis.');
                $signature = $value;
                continue;
            }
            if (!isset($fields[$key])) $fields[$key] = [];
            $fields[$key][] = $value;
        }
        if (!is_string($signature) || !preg_match('/^[a-f0-9]{64}$/iD', $signature)) {
            throw new InvalidArgumentException('Nedostaje valjani Shopify HMAC.');
        }
        $signed = [];
        foreach ($fields as $key => $values) $signed[] = $key . '=' . implode(',', $values);
        sort($signed, SORT_STRING);
        $digest = hash_hmac('sha256', implode('', $signed), $secret);
        if (!hash_equals(strtolower($signature), $digest)) {
            throw new InvalidArgumentException('Shopify potpis nije valjan.');
        }
        if (count($fields['timestamp'] ?? []) !== 1 ||
            !ctype_digit((string) $fields['timestamp'][0]) ||
            abs($now - (int) $fields['timestamp'][0]) > 300) {
            throw new InvalidArgumentException('Shopify zahtjev je istekao.');
        }
        if (count($fields['shop'] ?? []) !== 1) throw new InvalidArgumentException('Nepoznata trgovina.');
        $shop = self::shop($fields['shop'][0]);
        return ['shop' => $shop, 'parameters' => $fields];
    }

    public static function requireInstalled(string $shop, string $allowed): void
    {
        $installed = array_filter(array_map('trim', explode(',', strtolower($allowed))));
        if (!in_array($shop, $installed, true)) {
            throw new InvalidArgumentException('Shopify trgovina nema registriranu BAZNA instalaciju.');
        }
    }
}
