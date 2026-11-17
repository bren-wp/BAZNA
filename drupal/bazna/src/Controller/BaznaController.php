<?php

declare(strict_types=1);

namespace Drupal\bazna\Controller;

use Brendigo\Bazna\Core;
use Drupal\Core\Controller\ControllerBase;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * BAZNA read-only compatibility API (same published snapshots as Joomla/Web).
 *
 * Platform credentials and private file paths are never exposed to visitors.
 */
final class BaznaController extends ControllerBase {

  private function core(): Core {
    require_once dirname(__DIR__, 2) . '/vendor/Core.php';
    $private = (string) (getenv('BAZNA_DRUPAL_PRIVATE_DIR') ?: '');
    $url = (string) (getenv('BAZNA_DRUPAL_PUBLIC_URL') ?: '');
    $core = new Core($private, $url);
    $core->ensurePrivate();
    return $core;
  }

  private function result(string $type, string $body, int $status = 200, array $extra = []): Response {
    $response = new Response($body, $status, array_merge([
      'Content-Type' => $type,
      'Cache-Control' => 'no-store, max-age=0, must-revalidate',
      'X-Content-Type-Options' => 'nosniff',
      'Referrer-Policy' => 'no-referrer',
    ], $extra));
    return $response;
  }

  private function safe(callable $callback): Response {
    try {
      return $callback($this->core());
    }
    catch (\InvalidArgumentException $error) {
      return $this->json(['error' => 'invalid_parameters'], 400);
    }
    catch (\Throwable $error) {
      \Drupal::logger('bazna')->error('Public BAZNA request failed: @type', ['@type' => get_class($error)]);
      return $this->json(['error' => 'public_feed_unavailable'], 503);
    }
  }

  private function json(array $data, int $status = 200): Response {
    return $this->result('application/json; charset=utf-8',
      json_encode($data, JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES), $status);
  }

  public function discovery(): Response {
    return $this->safe(fn(Core $c) => $this->json($c->discovery('drupal')));
  }

  public function inquiries(Request $request): Response {
    return $this->safe(fn(Core $c) => $this->json($c->onRequestServices(
      (string) $request->query->get('outlet', ''),
      (string) $request->query->get('q', '')
    )));
  }

  public function archives(): Response {
    return $this->safe(fn(Core $c) => $this->json($c->archives()));
  }

  public function feed(string $kind, string $format, Request $request): Response {
    return $this->safe(function(Core $c) use ($kind, $format, $request): Response {
      $outlet = (string) $request->query->get('oznaka', '');
      return $this->result($format === 'csv' ? 'text/csv; charset=utf-8' : 'application/xml; charset=utf-8',
        $format === 'csv' ? $c->csv($kind, $outlet) : $c->xml($kind, $outlet));
    });
  }

  public function officialFeed(string $kind, string $format, Request $request): Response {
    return $this->safe(function(Core $c) use ($kind, $format, $request): Response {
      $outlet = (string) $request->query->get('oznaka', '');
      $name = $c->officialFilename($kind, $format, $outlet);
      return $this->result($format === 'csv' ? 'text/csv; charset=utf-8' : 'application/xml; charset=utf-8',
        $format === 'csv' ? $c->officialCsv($kind, $outlet) : $c->officialXml($kind, $outlet),
        200, ['Content-Disposition' => 'inline; filename="' . $name . '"']);
    });
  }

  public function catalog(): Response {
    return $this->safe(function(Core $c): Response {
      $name = Core::esc('BAZNA — Bazne cijene');
      $html = '<!doctype html><html lang="hr"><head><meta charset="utf-8">' .
        '<meta name="viewport" content="width=device-width, initial-scale=1">' .
        '<title>' . $name . '</title><style>' .
        'body{margin:0;background:#081525;color:#f6f8fc;font:16px system-ui}' .
        'main{max-width:1200px;margin:auto;padding:24px}' .
        '.bazna-panel{padding:16px;background:#12263c;border-radius:14px;margin:16px 0}' .
        'table{width:100%;border-collapse:collapse}td,th{padding:10px;text-align:left;border-bottom:1px solid #364b65}' .
        '@media(max-width:700px){table{display:block;max-width:100%;overflow:auto}}' .
        '</style></head><body><main><h1>' . $name . '</h1>' .
        $c->publicCatalogHtml('proizvodi') . $c->publicCatalogHtml('usluge') .
        '</main></body></html>';
      return $this->result('text/html; charset=utf-8', $html, 200,
        ['Content-Security-Policy' => "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'"]);
    });
  }
}
