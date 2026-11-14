<?php
/**
 * Joomla 5/6 BAZNA system plugin; public same-origin API and Joomla-authenticated
 * catalog administration. Vendor/Core.php is bundled in the ZIP.
 */
namespace Brendigo\Plugin\System\Bazna\Extension;
defined('_JEXEC') or die;

use Brendigo\Bazna\Core;
use Brendigo\Bazna\AdminCatalog;
use Joomla\CMS\Event\Application\AfterInitialiseEvent;
use Joomla\CMS\Plugin\CMSPlugin;
use Joomla\CMS\Uri\Uri;
use Joomla\CMS\Session\Session;
use Joomla\Event\SubscriberInterface;

final class Bazna extends CMSPlugin implements SubscriberInterface
{
    public static function getSubscribedEvents(): array
    {
        return ['onAfterInitialise' => 'handleRequest'];
    }

    public function handleRequest(AfterInitialiseEvent $event): void
    {
        $app = $event->getApplication();
        $admin = $app->isClient('administrator');
        if (!$admin && !$app->isClient('site')) return;
        $path = rawurldecode((string) parse_url((string) ($_SERVER['REQUEST_URI'] ?? '/'), PHP_URL_PATH));
        $sitePath = trim((string) Uri::root(true), '/');
        if ($sitePath !== '' && str_starts_with($path, '/' . $sitePath . '/')) {
            $path = substr($path, strlen($sitePath) + 1);
        }
        $dashboard = $admin && $app->input->getCmd('bazna', '') === 'dashboard';
        if (!$dashboard && ($admin || !str_starts_with($path, '/bazna/'))) return;
        $method = strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET'));
        if (!$dashboard && $method !== 'GET') {
            http_response_code(405);
            $app->close();
        }
        require_once dirname(__DIR__, 2) . '/vendor/Core.php';
        require_once dirname(__DIR__, 2) . '/vendor/AdminCatalog.php';
        try {
            $storage = (string) getenv('BAZNA_JOOMLA_PRIVATE_DIR');
            $base = (string) getenv('BAZNA_JOOMLA_PUBLIC_URL');
            if ($storage === '' || $base === '') throw new \RuntimeException('Konfigurirajte privatnu mapu i javni HTTPS URL.');
            $core = new Core($storage, $base);
            $core->ensurePrivate();
            if ($dashboard) {
                if (!$app->getIdentity()->authorise('core.admin')) {
                    http_response_code(403);
                    echo 'Administratorska ovlast je obvezna.';
                    $app->close();
                }
                $this->adminPage($core, $method);
                $app->close();
            }
            header('X-Content-Type-Options: nosniff');
            header('Cache-Control: no-store');
            if ($path === '/bazna/v1/cjenici') {
                $this->json($core->discovery('joomla'));
            } elseif ($path === '/bazna/v1/usluge-na-upit') {
                $this->json($core->onRequestServices(
                    (string) $app->input->getString('outlet', ''),
                    (string) $app->input->getString('q', '')
                ));
            } elseif ($path === '/bazna/v1/arhiva') {
                $this->json($core->archives());
            } elseif (preg_match('#^/bazna/v1/cjenici/(proizvodi|usluge)/(csv|xml)$#D', $path, $m)) {
                $outlet = (string) $app->input->getString('oznaka', '');
                header('Content-Type: ' . ($m[2] === 'csv' ? 'text/csv' : 'application/xml') . '; charset=utf-8');
                echo $m[2] === 'csv' ? $core->csv($m[1], $outlet) : $core->xml($m[1], $outlet);
            } elseif (preg_match('#^/bazna/v1/arhiva/([0-9]{14}-[a-f0-9]{12})/(proizvodi|usluge)/csv$#D', $path, $m)) {
                header('Content-Type: text/csv; charset=utf-8');
                echo $core->archiveCsv($m[1], $m[2], (string) $app->input->getString('oznaka', ''));
            } elseif ($path === '/bazna/cjenik' || $path === '/bazna/arhiva') {
                $this->publicPage($core, $path === '/bazna/arhiva');
            } else {
                http_response_code(404);
                $this->json(['error' => 'not_found']);
            }
        } catch (\InvalidArgumentException $e) {
            http_response_code(400);
            $this->json(['error' => $e->getMessage()]);
        } catch (\Throwable $e) {
            error_log('BAZNA Joomla: ' . $e->getMessage());
            http_response_code(503);
            $this->json(['error' => 'BAZNA nije dostupna.']);
        }
        $app->close();
    }

    private function json(array $result): void
    {
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode($result, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }

    private function adminPage(Core $core, string $method): void
    {
        $app = $this->getApplication();
        $status = '';
        if ($method === 'POST') {
            if (!Session::checkToken('post')) {
                http_response_code(403);
                echo 'CSRF provjera nije uspjela.';
                return;
            }
            $action = $app->input->post->getCmd('action');
            if (in_array($action, ['save', 'publish', 'save_item', 'delete_item'], true)) {
                if ($action === 'save') {
                    $raw = $app->input->post->get('catalog', '', 'raw');
                    if (!is_string($raw) || strlen($raw) > 6 * 1024 * 1024) throw new \InvalidArgumentException('Neispravan katalog.');
                    $catalog = json_decode($raw, true, 40, JSON_THROW_ON_ERROR);
                    if (!is_array($catalog)) throw new \InvalidArgumentException('Neispravan JSON.');
                    $core->saveDraft($catalog);
                    $status = 'Nacrt je spremljen.';
                } elseif ($action === 'publish') {
                    $core->publish();
                    $status = 'Cjenik je objavljen.';
                } else {
                    if ($action === 'delete_item' &&
                        (string) ($_POST['delete_confirm'] ?? '') !== '1') {
                        throw new \InvalidArgumentException('Potvrdite uklanjanje stavke.');
                    }
                    $index = AdminCatalog::indexFromPost($_POST);
                    $row = $action === 'save_item' ? AdminCatalog::fromPost($_POST) : null;
                    $core->modifyRow(
                        $app->input->post->getCmd('collection', ''),
                        $index, $row, (string) ($_POST['expected_hash'] ?? '')
                    );
                    $status = $action === 'save_item' ? 'Stavka je spremljena.' : 'Stavka je uklonjena.';
                }
            }
        }
        $draft = $core->read('draft.json');
        $tab = $app->input->getCmd('tab', 'pregled');
        $links = ['pregled' => 'Pregled', 'proizvodi' => 'Proizvodi',
            'usluge' => 'Usluge', 'na-upit' => 'Cijena na upit', 'poslovnice' => 'Poslovnice',
            'uvoz-izvoz' => 'Uvoz/Izvoz', 'javni-cjenici' => 'Javni cjenici',
            'arhiva' => 'Arhiva', 'postavke' => 'Postavke'];
        if (!isset($links[$tab])) $tab = 'pregled';
        header('Content-Type: text/html; charset=utf-8');
        echo '<!doctype html><html lang="hr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">';
        echo '<title>BAZNA — Joomla</title><link rel="stylesheet" href="' . Core::esc(Uri::root() . 'plugins/system/bazna/assets/bazna.css') . '"></head>';
        echo '<body class="bazna-admin"><header class="bazna-topbar"><strong>BAZNA · Joomla</strong><a href="' . Core::esc($core->link('bazna/cjenik')) . '">Javni cjenik</a></header><div class="bazna-workspace"><nav class="bazna-sidebar">';
        foreach ($links as $id => $text) {
            echo '<a href="?bazna=dashboard&amp;tab=' . Core::esc($id) . '"' .
                ($tab === $id ? ' aria-current="page"' : '') . '>' . Core::esc($text) . '</a>';
        }
        echo '</nav><main class="bazna-main"><h1>' . Core::esc($links[$tab]) . '</h1>';
        if ($status !== '') echo '<p role="status" class="bazna-message">' . Core::esc($status) . '</p>';
        echo '<div class="bazna-panel">';
        if (in_array($tab, ['proizvodi', 'usluge', 'poslovnice', 'na-upit'], true)) {
            echo AdminCatalog::render(
                $tab, $draft, '?bazna=dashboard&tab=' . $tab,
                \Joomla\CMS\HTML\HTMLHelper::_('form.token'),
                $app->input->getInt('page', 1)
            );
        } elseif ($tab === 'uvoz-izvoz') {
            $json = json_encode(array_intersect_key($draft, array_flip(['products','services','outlets','on_request'])),
                JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
            echo '<h2>Uvoz i izvoz JSON kataloga</h2><p>Za pojedinačne izmjene koristite kartice Proizvodi, Usluge i Poslovnice.</p><form method="post">';
            echo \Joomla\CMS\HTML\HTMLHelper::_('form.token');
            echo '<input type="hidden" name="action" value="save"><textarea class="bazna-editor" name="catalog">' . Core::esc((string) $json) . '</textarea>';
            echo '<button type="submit">Spremi nacrt</button></form>';
        } elseif ($tab === 'javni-cjenici') {
            echo '<p>Android REST: <code>' . Core::esc($core->link('bazna/v1/cjenici')) . '</code></p><form method="post">';
            echo \Joomla\CMS\HTML\HTMLHelper::_('form.token');
            echo '<button name="action" value="publish">Objavi cjenike</button></form>';
        } elseif ($tab === 'arhiva') {
            foreach ($core->archives() as $a) echo '<p><a href="' . Core::esc($a['url']) . '">' . Core::esc($a['date'].' '.$a['kind']) . '</a></p>';
        } else {
            echo '<p>Proizvodi: ' . count($draft['products'] ?? []) . ' · Usluge: ' .
                count($draft['services'] ?? []) . ' · Poslovnice: ' . count($draft['outlets'] ?? []) . ' · Na upit: ' . count($draft['on_request'] ?? []) . '</p>';
            echo '<p>Datum početka primjene objavljenih odluka: 17. 11. 2026. Bazna cijena mora biti potvrđena povijesnom evidencijom.</p>';
        }
        echo '</div></main></div></body></html>';
    }

    private function publicPage(Core $core, bool $archive): void
    {
        header('Content-Type: text/html; charset=utf-8');
        echo '<!doctype html><html lang="hr"><head><meta name="viewport" content="width=device-width,initial-scale=1"><meta charset="utf-8"><title>BAZNA cjenici</title>';
        echo '<link rel="stylesheet" href="' . Core::esc(Uri::root() . 'plugins/system/bazna/assets/bazna.css') . '"></head><body class="bazna-public">';
        echo '<main class="bazna-public-main"><h1>BAZNA — ' . ($archive ? 'Arhiva' : 'Javni cjenici') . '</h1>';
        if ($archive) {
            foreach ($core->archives() as $entry) echo '<p><a href="' . Core::esc($entry['url']) . '">' .
                Core::esc($entry['date'] . ' — ' . $entry['kind']) . '</a></p>';
        } else {
            $outlet = (string) $this->getApplication()->input->getString('oznaka', '');
            $query = (string) $this->getApplication()->input->getString('q', '');
            if (strlen($query) > 100) $query = function_exists('mb_strcut')
                ? mb_strcut($query, 0, 100, 'UTF-8') : substr($query, 0, 100);
            foreach (Core::KINDS as $kind) {
                echo '<section class="bazna-panel"><h2>' . Core::esc(
                    $kind === 'proizvodi' ? 'Proizvodi' : 'Usluge'
                ) . '</h2><a href="' .
                    Core::esc($core->link('bazna/v1/cjenici/' . $kind . '/csv' .
                        ($outlet === '' ? '' : '?oznaka=' . rawurlencode($outlet)))) .
                    '">Preuzmi CSV</a></section>';
                echo $core->publicCatalogHtml($kind, $outlet, $query);
            }
        }
        echo '</main></body></html>';
    }
}
