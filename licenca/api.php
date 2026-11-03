<?php
/**
 * BAZNA licensing public endpoint. Upload this whole /licenca directory to
 * licenca.brendigo.com, provision private/config.php (never in public Git).
 */
declare(strict_types=1);
define('BAZNA_LICENSE_INTERNAL', true);
require_once __DIR__ . '/lib.php';
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, max-age=0');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');
header('X-Frame-Options: DENY');
header('Access-Control-Allow-Origin: none');

function bazna_respond(int $status, array $data): void {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_SLASHES);
    exit;
}

if (empty($_SERVER['HTTPS']) || $_SERVER['HTTPS'] === 'off') {
    // Reverse proxy must set HTTPS correctly at the PHP gateway.
    bazna_respond(403, array('ok' => false, 'code' => 'https_required'));
}
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'GET') {
    bazna_respond(200, array('name' => 'BAZNA License API', 'version' => 1, 'status' => 'ready'));
}
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    header('Allow: GET, POST');
    bazna_respond(405, array('ok' => false, 'code' => 'method_not_allowed'));
}
$length = (int) ($_SERVER['CONTENT_LENGTH'] ?? 0);
if ($length < 1 || $length > 2048) {
    bazna_respond(413, array('ok' => false, 'code' => 'invalid_request_size'));
}
$request = json_decode((string) file_get_contents('php://input'), true);
if (!is_array($request)) {
    bazna_respond(400, array('ok' => false, 'code' => 'invalid_json'));
}
$configPath = __DIR__ . '/private/config.php';
if (!is_file($configPath)) {
    bazna_respond(503, array('ok' => false, 'code' => 'server_not_provisioned'));
}
$config = require $configPath;
if (!is_array($config) || !isset($config['master_secret_hex']) ||
    !is_string($config['master_secret_hex']) || !preg_match('/^[a-f0-9]{64}$/i', $config['master_secret_hex'])) {
    bazna_respond(503, array('ok' => false, 'code' => 'server_configuration_error'));
}
$storage = isset($config['storage_dir']) && is_string($config['storage_dir'])
    ? $config['storage_dir'] : dirname(__DIR__, 2) . '/bazna-license-private';
try {
    // Do not trust X-Forwarded-For: rate limit on the actual PHP peer address.
    $ip = (string) ($_SERVER['REMOTE_ADDR'] ?? '');
    bazna_license_rate_limit($storage, $ip);
    foreach (array('action', 'key', 'host') as $name) {
        if (!isset($request[$name]) || !is_string($request[$name])) {
            bazna_respond(400, array('ok' => false, 'code' => 'invalid_request'));
        }
    }
    $out = bazna_license_action(
        $config['master_secret_hex'], $storage,
        $request['action'], $request['key'], $request['host'],
        isset($request['site_path']) && is_string($request['site_path']) ? $request['site_path'] : '/',
        isset($request['challenge']) && is_string($request['challenge']) ? $request['challenge'] : ''
    );
    $code = $out['code'] ?? 'error';
    $status = !empty($out['ok']) ? 200 :
        (in_array($code, array('already_used_on_another_domain', 'not_activated_for_domain'), true) ? 409 :
        ($code === 'domain_proof_failed' ? 403 : 400));
    bazna_respond($status, $out);
} catch (InvalidArgumentException $e) {
    bazna_respond(400, array('ok' => false, 'code' => $e->getMessage()));
} catch (RuntimeException $e) {
    if ($e->getMessage() === 'rate_limit_exceeded') {
        bazna_respond(429, array('ok' => false, 'code' => 'rate_limit_exceeded'));
    }
    error_log('BAZNA licensing storage failure');
    bazna_respond(503, array('ok' => false, 'code' => 'license_service_unavailable'));
} catch (Throwable $e) {
    error_log('BAZNA licensing unexpected failure');
    bazna_respond(503, array('ok' => false, 'code' => 'license_service_unavailable'));
}
