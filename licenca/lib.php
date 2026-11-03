<?php
/**
 * BAZNA License Server — standalone PHP, JSON storage, no SQL or third-party
 * services. Public endpoint: https://licenca.brendigo.com/licenca/api.php.
 * Production secret is deployed privately; NEVER commit it to GitHub.
 */
declare(strict_types=1);
defined('BAZNA_LICENSE_INTERNAL') || exit;

function bazna_license_key(string $secret, int $serial): string {
    if (!preg_match('/^[a-f0-9]{64}$/i', $secret) || $serial < 1 || $serial > 10000) {
        throw new InvalidArgumentException('Invalid secret or license serial');
    }
    $mac = strtoupper(substr(hash_hmac('sha256', 'BAZNA-LICENCE-V1:' . $serial, hex2bin($secret)), 0, 32));
    return sprintf('BAZNA-%05d-%s-%s-%s-%s-%s-%s-%s-%s', $serial,
        substr($mac, 0, 4), substr($mac, 4, 4), substr($mac, 8, 4),
        substr($mac, 12, 4), substr($mac, 16, 4), substr($mac, 20, 4),
        substr($mac, 24, 4), substr($mac, 28, 4));
}

function bazna_license_serial(string $secret, string $key): int {
    $key = strtoupper(trim($key));
    if (!preg_match('/^BAZNA-(\d{5})(?:-[A-F0-9]{4}){8}$/D', $key, $match)) {
        return 0;
    }
    $serial = (int) $match[1];
    if ($serial < 1 || $serial > 10000) {
        return 0;
    }
    return hash_equals(bazna_license_key($secret, $serial), $key) ? $serial : 0;
}

function bazna_license_domain(string $host): string {
    $host = strtolower(trim($host));
    // A hostname, not a URL: no user info, path, private IPs, ports or unicode.
    if (strlen($host) > 253 || !preg_match('/^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}$/D', $host)) {
        throw new InvalidArgumentException('invalid_domain');
    }
    if (filter_var($host, FILTER_VALIDATE_IP)) {
        throw new InvalidArgumentException('ip_domain_not_supported');
    }
    return preg_replace('/^www\./', '', $host);
}

function bazna_license_site_path(string $path): string {
    if ($path === '') {
        return '/';
    }
    if (strlen($path) > 120 || !preg_match('#^/(?:[a-zA-Z0-9_.~%-]+/)*$#D', $path) ||
        strpos($path, '..') !== false || strpos($path, '%') !== false) {
        throw new InvalidArgumentException('invalid_site_path');
    }
    return $path;
}

function bazna_license_public_ip(string $ip): bool {
    return (bool) filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_IPV4 | FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE);
}

/**
 * Reject JSON storage in DOCUMENT_ROOT, including not-yet-created directories.
 * Walk to the nearest real parent to avoid a missing-path check bypass.
 */
function bazna_license_storage_is_private(string $webRoot, string $storage): bool {
    $root = realpath($webRoot);
    if ($root === false || $storage === '' ||
        !str_starts_with($storage, DIRECTORY_SEPARATOR)) {
        return false;
    }
    $probe = $storage;
    for ($i = 0; $i < 32 && !file_exists($probe); ++$i) {
        $parent = dirname($probe);
        if ($parent === $probe) {
            return false;
        }
        $probe = $parent;
    }
    $base = realpath($probe);
    if ($base === false) {
        return false;
    }
    $prefix = rtrim($root, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR;
    return $base !== $root && strncmp($base, $prefix, strlen($prefix)) !== 0;
}

/**
 * Prove that the claimant controls the publicly reachable HTTPS WordPress
 * installation. CURLOPT_RESOLVE pins the already validated public DNS A
 * address, preventing DNS rebinding SSRF. No redirects or private IPs.
 */
function bazna_license_proof(string $hostname, string $sitePath, string $challenge): bool {
    if (!function_exists('curl_init')) {
        return false;
    }
    $ips = gethostbynamel($hostname);
    if (!is_array($ips) || !$ips) {
        return false;
    }
    $ip = '';
    foreach ($ips as $candidate) {
        if (!bazna_license_public_ip($candidate)) {
            return false; // fail closed on mixed public/private DNS
        }
        if ($ip === '') {
            $ip = $candidate;
        }
    }
    if ($ip === '') {
        return false;
    }
    $routes = array(
        $sitePath . 'wp-json/brendigo-sidrena/v1/license-proof',
        $sitePath . '?rest_route=%2Fbrendigo-sidrena%2Fv1%2Flicense-proof',
    );
    foreach ($routes as $route) {
        $url = 'https://' . $hostname . $route;
        $curl = curl_init($url);
        if ($curl === false) {
            continue;
        }
        $body = '';
        curl_setopt_array($curl, array(
            CURLOPT_RETURNTRANSFER => false,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_CONNECTTIMEOUT => 4,
            CURLOPT_TIMEOUT => 7,
            CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_RESOLVE => array($hostname . ':443:' . $ip),
            CURLOPT_HTTPHEADER => array('Accept: application/json'),
            CURLOPT_USERAGENT => 'BAZNA-License-Verification/1.0',
            CURLOPT_WRITEFUNCTION => static function ($ch, string $chunk) use (&$body): int {
                if (strlen($body) + strlen($chunk) > 8192) {
                    return 0;
                }
                $body .= $chunk;
                return strlen($chunk);
            },
        ));
        curl_exec($curl);
        $status = (int) curl_getinfo($curl, CURLINFO_HTTP_CODE);
        $error = curl_errno($curl);
        curl_close($curl);
        if ($error === 0 && $status === 200) {
            $json = json_decode($body, true);
            if (is_array($json) && isset($json['challenge']) &&
                is_string($json['challenge']) && hash_equals($challenge, $json['challenge'])) {
                return true;
            }
        }
    }
    return false;
}

/**
 * File lock plus same-directory atomic rename: concurrency-safe one-domain
 * registration and robust against partial JSON writes.
 */
function bazna_license_store(string $dir, callable $handler): array {
    if (!is_dir($dir) && !mkdir($dir, 0700, true) && !is_dir($dir)) {
        throw new RuntimeException('storage_not_writable');
    }
    $lock = fopen($dir . '/activation.lock', 'c');
    if (!$lock || !flock($lock, LOCK_EX)) {
        throw new RuntimeException('license_lock_failed');
    }
    try {
        $file = $dir . '/activations.json';
        if (is_file($file)) {
            $raw = file_get_contents($file);
            $data = json_decode((string) $raw, true);
            if (!is_array($data)) {
                throw new RuntimeException('invalid_licence_storage');
            }
        } else {
            $data = array();
        }
        $changed = false;
        $result = $handler($data, $changed);
        if ($changed) {
            $tmp = tempnam($dir, 'bazna-');
            if ($tmp === false) {
                throw new RuntimeException('atomic_write_failed');
            }
            try {
                chmod($tmp, 0600);
                $json = json_encode($data, JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT | JSON_THROW_ON_ERROR);
                if (file_put_contents($tmp, $json) === false || !rename($tmp, $file)) {
                    throw new RuntimeException('atomic_write_failed');
                }
            } finally {
                if (is_file($tmp)) {
                    unlink($tmp);
                }
            }
        }
        return $result;
    } finally {
        flock($lock, LOCK_UN);
        fclose($lock);
    }
}

/** Per-peer sliding-minute limit. IP is hashed; only transient counts persist. */
function bazna_license_rate_limit(string $dir, string $remoteIp): void {
    if (!is_dir($dir) && !mkdir($dir, 0700, true) && !is_dir($dir)) {
        throw new RuntimeException('storage_not_writable');
    }
    $handle = fopen($dir . '/rate.lock', 'c');
    if (!$handle || !flock($handle, LOCK_EX)) {
        throw new RuntimeException('rate_limit_storage_failure');
    }
    try {
        $path = $dir . '/rate.json';
        if (is_file($path)) {
            $raw = file_get_contents($path);
            $state = is_string($raw) ? json_decode($raw, true) : null;
            if (!is_array($state)) {
                // Corrupt storage must not silently reset brute-force limits.
                throw new RuntimeException('rate_limit_storage_failure');
            }
        } else {
            $state = array();
        }
        $now = time();
        foreach ($state as $hash => $entry) {
            if (!is_array($entry) || ($entry['minute'] ?? 0) < $now - 120) {
                unset($state[$hash]);
            }
        }
        $key = hash('sha256', $remoteIp);
        $entry = $state[$key] ?? array('minute' => $now, 'count' => 0);
        if (!is_array($entry) || $now - (int) ($entry['minute'] ?? 0) >= 60) {
            $entry = array('minute' => $now, 'count' => 0);
        }
        $entry['count'] = (int) ($entry['count'] ?? 0) + 1;
        $state[$key] = $entry;
        $tmp = tempnam($dir, 'rate-');
        if ($tmp === false) {
            throw new RuntimeException('rate_limit_storage_failure');
        }
        try {
            chmod($tmp, 0600);
            if (file_put_contents($tmp, json_encode($state, JSON_THROW_ON_ERROR)) === false ||
                !rename($tmp, $path)) {
                throw new RuntimeException('rate_limit_storage_failure');
            }
        } finally {
            if (is_file($tmp)) {
                unlink($tmp);
            }
        }
        if ($entry['count'] > 60) {
            throw new RuntimeException('rate_limit_exceeded');
        }
    } finally {
        flock($handle, LOCK_UN);
        fclose($handle);
    }
}

function bazna_license_action(string $secret, string $dir, string $action, string $key,
    string $hostname, string $path, string $challenge = '', ?callable $proof = null): array {
    $serial = bazna_license_serial($secret, $key);
    if (!$serial) {
        return array('ok' => false, 'code' => 'invalid_key');
    }
    $domain = bazna_license_domain($hostname);
    $path = bazna_license_site_path($path);
    if (!in_array($action, array('activate', 'check', 'deactivate'), true)) {
        return array('ok' => false, 'code' => 'invalid_action');
    }
    if ($action !== 'check') {
        if (!preg_match('/^[a-f0-9]{64}$/D', $challenge)) {
            return array('ok' => false, 'code' => 'invalid_challenge');
        }
        $verify = $proof ?: 'bazna_license_proof';
        if (!$verify($hostname, $path, $challenge)) {
            return array('ok' => false, 'code' => 'domain_proof_failed');
        }
    }
    return bazna_license_store($dir, static function (array &$data, bool &$changed) use ($action, $serial, $domain): array {
        $id = (string) $serial;
        $existing = isset($data[$id]) && is_array($data[$id]) ? $data[$id] : null;
        // A serial belongs to ONE canonical domain for its lifetime. Even
        // deactivation retains the binding: it is not a public transfer API.
        // A transfer, if ever allowed by policy, requires a separate audited
        // server-side administrator operation (never this public endpoint).
        if ($existing !== null && ($existing['domain'] ?? '') !== $domain) {
            return array('ok' => false, 'code' => 'already_used_on_another_domain');
        }
        if ($action === 'check') {
            if ($existing === null || ($existing['status'] ?? 'active') !== 'active') {
                return array('ok' => false, 'code' => 'not_activated_for_domain');
            }
            // Record successful checks but avoid rewriting JSON on each call.
            $last = isset($existing['last_checked_unix']) ? (int) $existing['last_checked_unix'] : 0;
            if ($last < time() - 3600) {
                $data[$id]['checked_at'] = gmdate('c');
                $data[$id]['last_checked_unix'] = time();
                $changed = true;
            }
            return array('ok' => true, 'code' => 'active', 'domain' => $domain);
        }
        if ($action === 'deactivate') {
            if ($existing === null || ($existing['status'] ?? 'active') !== 'active') {
                return array('ok' => false, 'code' => 'not_activated_for_domain');
            }
            $data[$id]['status'] = 'inactive';
            $data[$id]['deactivated_at'] = gmdate('c');
            $changed = true;
            return array('ok' => true, 'code' => 'deactivated', 'domain' => $domain);
        }
        $data[$id] = array(
            'domain' => $domain,
            'status' => 'active',
            'activated_at' => $existing['activated_at'] ?? gmdate('c'),
            'checked_at' => gmdate('c'),
            'last_checked_unix' => time(),
        );
        $changed = true;
        return array('ok' => true, 'code' => 'active', 'domain' => $domain);
    });
}
