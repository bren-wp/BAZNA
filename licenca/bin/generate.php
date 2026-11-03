<?php
/**
 * Run via CLI: php licenca/bin/generate.php /secure/private/path/BAZNA-10000.txt
 * Reproducible keys from deployment-only random master key; no external service.
 */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    exit;
}
define('BAZNA_LICENSE_INTERNAL', true);
require_once dirname(__DIR__) . '/lib.php';
$configPath = dirname(__DIR__) . '/private/config.php';
if (!is_file($configPath)) {
    fwrite(STDERR, "First create licenca/private/config.php from a securely generated 256-bit secret.\n");
    exit(1);
}
$config = require $configPath;
$master = isset($config['master_secret_hex']) ? (string) $config['master_secret_hex'] : '';
if (!preg_match('/^[a-f0-9]{64}$/i', $master)) {
    fwrite(STDERR, "Invalid private master secret.\n");
    exit(1);
}
$out = $argv[1] ?? '';
if (!$out || file_exists($out)) {
    fwrite(STDERR, "Provide a new output filename outside web root; will not overwrite.\n");
    exit(1);
}
$handle = fopen($out, 'x');
if ($handle === false) {
    fwrite(STDERR, "Cannot create private output file.\n");
    exit(1);
}
chmod($out, 0600);
fwrite($handle, "BAZNA — 10.000 licencnih ključeva / PRIVATNO\n");
for ($i = 1; $i <= 10000; ++$i) {
    fwrite($handle, bazna_license_key($master, $i) . "\n");
}
fclose($handle);
fwrite(STDOUT, "Generated 10000 unique licenses into a private file.\n");
