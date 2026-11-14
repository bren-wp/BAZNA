<?php
/** Public BAZNA HTML cjenik and archive, from published snapshots only. */
declare(strict_types=1);
$isArchive = $path === '/bazna/arhiva';
$kind = (string) ($_GET['kind'] ?? 'proizvodi');
if (!in_array($kind, \Brendigo\Bazna\Core::KINDS, true)) $kind = 'proizvodi';
$outlet = (string) ($_GET['oznaka'] ?? '');
$search = isset($_GET['q']) && is_string($_GET['q']) ? trim($_GET['q']) : '';
if (strlen($search) > 100) $search = function_exists('mb_strcut')
    ? mb_strcut($search, 0, 100, 'UTF-8') : substr($search, 0, 100);
header("Content-Security-Policy: default-src 'none'; style-src 'self'; base-uri 'none'; frame-ancestors 'none'");
?><!doctype html><html lang="hr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>BAZNA — <?= $isArchive ? 'Arhiva' : 'Javni cjenik' ?></title>
<link rel="stylesheet" href="<?= bazna_html($core->link('bazna/assets/bazna.css')) ?>"></head><body class="bazna-public">
<header class="bazna-topbar"><div><span class="bazna-mark">B</span> <strong>BAZNA</strong><small>Bazne cijene by Brendigo</small></div>
<nav><a href="<?= bazna_html($core->link('bazna/cjenik')) ?>">Cjenik</a> · <a href="<?= bazna_html($core->link('bazna/arhiva')) ?>">Arhiva</a></nav></header>
<main class="bazna-public-main"><p class="bazna-eyebrow">BAZNA / JAVNI PODACI</p><h1><?= $isArchive ? 'Arhiva javnih cjenika' : 'Objavljene bazne cijene' ?></h1>
<p class="bazna-muted">Datum posljednje objave: <?= bazna_html((string) ($records['published_at'] ?? 'nije objavljeno')) ?>. Iznosi su informativni, a povijesne vrijednosti moraju biti provjerene.</p>
<?php if ($isArchive): ?>
<div class="bazna-panel"><h2>Prethodne objave</h2>
<?php foreach ($core->archives() as $entry): ?><p><a href="<?= bazna_html($entry['url']) ?>"><?= bazna_html($entry['date'] . ' · ' . $entry['kind'] . ' · CSV') ?></a></p><?php endforeach; ?>
</div>
<?php else: ?>
<div class="bazna-switch"><a href="<?= bazna_html($core->link('bazna/cjenik?kind=proizvodi')) ?>">Proizvodi</a>
<a href="<?= bazna_html($core->link('bazna/cjenik?kind=usluge')) ?>">Usluge</a>
<a href="<?= bazna_html($core->link('bazna/v1/cjenici/' . $kind . '/csv')) ?>">Preuzmi CSV</a></div>
<form method="get" class="bazna-panel">
<input type="hidden" name="kind" value="<?= bazna_html($kind) ?>">
<label>Pretraži cijeli cjenik <input type="search" name="q" maxlength="100" value="<?= bazna_html($search) ?>"></label>
<label>Poslovnica <select name="oznaka">
<option value=""<?= $outlet === '' ? ' selected' : '' ?>>Webshop</option>
<?php foreach ($records['outlets'] ?? [] as $branch):
$code = (string) ($branch['code'] ?? '');
if ($code === '') continue; ?>
<option value="<?= bazna_html($code) ?>"<?= $outlet === $code ? ' selected' : '' ?>><?= bazna_html((string) ($branch['name'] ?? $code)) ?></option>
<?php endforeach; ?>
</select></label><button type="submit">Prikaži</button></form>
<?= $core->publicCatalogHtml($kind, $outlet, $search) ?>
<?php endif; ?>
<p class="bazna-small">Bazna cijena nije automatski jednaka najnižoj cijeni tijekom 30 dana prije sniženja. <a href="<?= bazna_html($core->link('bazna/v1/cjenici')) ?>">Javni BAZNA API</a></p>
</main><footer class="bazna-footer">BAZNA · Brendigo · brendigo.com</footer></body></html>
