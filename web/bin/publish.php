<?php
/**
 * BAZNA scheduled publisher for shared hosting / PHP CLI.
 *
 * cron (every minute, example):
 * * * * * /usr/bin/php /absolute/path/to/bazna-web/bin/publish.php >> /private/logs/bazna-publish.log 2>&1
 *
 * Required server-only env: BAZNA_PRIVATE_DIR, BAZNA_PUBLIC_URL, BAZNA_TRADER_OIB|BAZNA_TRADER_MB.
 * Never expose this file over HTTP. Missing verification data must block release.
 */
declare(strict_types=1);

use Brendigo\Bazna\Core;

if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}
require_once dirname(__DIR__) . '/src/Core.php';

try {
    $private = (string) (getenv('BAZNA_PRIVATE_DIR') ?: '');
    $public = (string) (getenv('BAZNA_PUBLIC_URL') ?: '');
    $core = new Core($private, $public);
    $core->ensurePrivate();
    $before = $core->read('published.json');
    $after = $core->publish(true, true);
    $changed = ($before['publication_sequence'] ?? null) !== ($after['publication_sequence'] ?? null);
    echo 'BAZNA ' . ($changed ? 'published' : 'unchanged') .
        ' ' . (string) ($after['published_at'] ?? '') . PHP_EOL;
} catch (Throwable $exception) {
    // Private CLI log; never return server paths or internal details over HTTP.
    fwrite(STDERR, 'BAZNA publication blocked: ' . $exception->getMessage() . PHP_EOL);
    exit(1);
}
