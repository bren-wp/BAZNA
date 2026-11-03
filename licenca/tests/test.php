<?php
/** Offline deterministic regression tests; never access the public Internet. */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
define('BAZNA_LICENSE_INTERNAL', true);
require_once dirname(__DIR__) . '/lib.php';

function expect_bazna(bool $ok, string $message): void {
    if (!$ok) {
        throw new RuntimeException('BAZNA test failed: ' . $message);
    }
}
$master = str_repeat('ab', 32);
$seen = array();
for ($i = 1; $i <= 10000; ++$i) {
    $key = bazna_license_key($master, $i);
    expect_bazna(bazna_license_serial($master, $key) === $i, 'key verification ' . $i);
    expect_bazna(!isset($seen[$key]), 'unique key ' . $i);
    $seen[$key] = true;
}
expect_bazna(bazna_license_serial($master, 'BAZNA-00001-0000-0000-0000-0000-0000-0000-0000-0000') === 0,
    'rejected forged key');
expect_bazna(!bazna_license_public_ip('127.0.0.1'), 'loopback');
expect_bazna(!bazna_license_public_ip('192.168.1.5'), 'private range');
expect_bazna(bazna_license_public_ip('1.1.1.1'), 'public IP');
expect_bazna(bazna_license_domain('WWW.Primjer.hr') === 'primjer.hr', 'www alias');
try {
    bazna_license_domain('http://primjer.hr:8443/path');
    throw new RuntimeException('invalid domain accepted');
} catch (InvalidArgumentException $ignored) {
}
try {
    bazna_license_site_path('/../../admin/');
    throw new RuntimeException('path traversal accepted');
} catch (InvalidArgumentException $ignored) {
}
$base = sys_get_temp_dir() . '/bazna-license-test-' . bin2hex(random_bytes(6));
mkdir($base, 0700);
$challenge = str_repeat('a', 64);
$proof = static function ($host, $path, $token) use ($challenge): bool {
    return $token === $challenge && $path === '/';
};
$key = bazna_license_key($master, 1);
try {
    $activated = bazna_license_action($master, $base, 'activate', $key, 'www.primjer.hr', '/', $challenge, $proof);
    expect_bazna($activated['ok'] && $activated['domain'] === 'primjer.hr', 'first domain bound');
    $repeat = bazna_license_action($master, $base, 'activate', $key, 'primjer.hr', '/', $challenge, $proof);
    expect_bazna($repeat['ok'], 'same domain reactivation');
    $second = bazna_license_action($master, $base, 'activate', $key, 'druga.hr', '/', $challenge, $proof);
    expect_bazna(!$second['ok'] && $second['code'] === 'already_used_on_another_domain', 'different domain blocked');
    $status = bazna_license_action($master, $base, 'check', $key, 'primjer.hr', '/');
    expect_bazna($status['ok'], 'same domain check');
    $otherStatus = bazna_license_action($master, $base, 'check', $key, 'druga.hr', '/');
    expect_bazna(!$otherStatus['ok'], 'second domain check blocked');
    $badProof = bazna_license_action($master, $base, 'deactivate', $key, 'primjer.hr', '/', $challenge, static function () {
        return false;
    });
    expect_bazna(!$badProof['ok'], 'domain proof required to release');
    $released = bazna_license_action($master, $base, 'deactivate', $key, 'primjer.hr', '/', $challenge, $proof);
    expect_bazna($released['ok'], 'deactivate original domain');
    $inactive = bazna_license_action($master, $base, 'check', $key, 'primjer.hr', '/');
    expect_bazna(!$inactive['ok'], 'deactivated license cannot unlock WordPress');
    $moved = bazna_license_action($master, $base, 'activate', $key, 'druga.hr', '/', $challenge, $proof);
    expect_bazna(!$moved['ok'] && $moved['code'] === 'already_used_on_another_domain',
        'deactivation cannot rebind key to another domain');
    $renewed = bazna_license_action($master, $base, 'activate', $key, 'primjer.hr', '/', $challenge, $proof);
    expect_bazna($renewed['ok'], 'reactivation allowed only on original domain');
    $record = json_decode((string) file_get_contents($base . '/activations.json'), true);
    expect_bazna(is_array($record) && $record['1']['domain'] === 'primjer.hr', 'permanent JSON domain binding');
    expect_bazna($record['1']['status'] === 'active', 'server records current license status');
    expect_bazna(isset($record['1']['activated_at'], $record['1']['checked_at']),
        'server records binding and verification dates');
    bazna_license_rate_limit($base, '203.0.113.15');
    $limitData = json_decode((string) file_get_contents($base . '/rate.json'), true);
    expect_bazna(is_array($limitData) && count($limitData) === 1, 'JSON rate limiter');
    // A corrupted limiter must fail closed, not reset counts to zero.
    file_put_contents($base . '/rate.json', '{broken-json');
    try {
        bazna_license_rate_limit($base, '203.0.113.15');
        throw new RuntimeException('Corrupt rate registry unexpectedly accepted');
    } catch (RuntimeException $expected) {
        expect_bazna($expected->getMessage() === 'rate_limit_storage_failure',
            'corrupt rate registry must fail closed');
    }
    echo "PASS: BAZNA 10,000 unique keys, permanent domain binding, proof, deactivation and JSON registry\n";
} finally {
    foreach (glob($base . '/*') as $file) {
        if (is_file($file)) {
            unlink($file);
        }
    }
    rmdir($base);
}
