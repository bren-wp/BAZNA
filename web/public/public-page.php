<?php
/** Public BAZNA HTML cjenik and archive, from published snapshots only. */
declare(strict_types=1);
$isArchive = $path === '/bazna/arhiva';
$kind = (string) ($_GET['kind'] ?? 'proizvodi');
if (!in_array($kind, \Brendigo\Bazna\Core::KINDS, true)) $kind = 'proizvodi';
$outlet = (string) ($_GET['oznaka'] ?? '');
$items = $records[$kind === 'proizvodi' ? 'products' : 'services'] ?? [];
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
<div class="bazna-panel bazna-table-scroll"><table><thead><tr><th>Naziv</th><th>Šifra</th><th>Redovna cijena</th><th>Bazna cijena</th><th>Referentni datum</th><th>Dostupnost</th></tr></thead><tbody>
<?php foreach ($items as $item): if (($item['outlet'] ?? '') !== $outlet) continue; ?>
<tr><td data-label="Naziv"><?= bazna_html($item['naziv']) ?></td>
<td data-label="Šifra"><?= bazna_html($item['sku']) ?></td>
<td data-label="Redovna cijena"><?= bazna_html($item['maloprodajna_cijena']) ?> €</td>
<td data-label="Bazna cijena"><?= bazna_html($item['sidrena_cijena']) ?><?= $item['sidrena_cijena'] === '' ? '—' : ' €' ?></td>
<td data-label="Referentni datum"><?= bazna_html($item['sidrena_datum']) ?></td>
<td data-label="Dostupnost"><?= bazna_html($item['dostupnost']) ?></td></tr>
<?php endforeach; ?></tbody></table></div>
<?php if ($kind === 'usluge'):
$requests = $core->onRequestServices()['items'];
$search = trim((string) ($_GET['q'] ?? ''));
if (strlen($search) > 100) $search = substr($search, 0, 100);
?>
<section class="bazna-panel"><h2>Cijena na upit</h2>
<p class="bazna-muted">Za ove usluge cijena se određuje prema navedenim kriterijima; nema izmišljenog iznosa u javnim CSV/XML datotekama.</p>
<form method="get"><input type="hidden" name="kind" value="usluge">
<?php if ($outlet !== ''): ?><input type="hidden" name="oznaka" value="<?= bazna_html($outlet) ?>"><?php endif; ?>
<label>Pretraži usluge na upit <input type="search" name="q" maxlength="100" value="<?= bazna_html($search) ?>"></label>
<button type="submit">Pretraži</button></form>
<div class="bazna-table-scroll"><table><thead><tr><th>Naziv usluge</th><th>Kriteriji</th><th>Cijena</th></tr></thead><tbody>
<?php foreach ($requests as $item):
if ($item['outlet'] !== $outlet) continue;
if ($search !== '' && (function_exists('mb_stripos')
    ? mb_stripos($item['name'] . ' ' . $item['criteria'], $search, 0, 'UTF-8') === false
    : stripos($item['name'] . ' ' . $item['criteria'], $search) === false)) continue; ?>
<tr><td data-label="Naziv"><?= bazna_html($item['name']) ?></td>
<td data-label="Kriteriji"><?= bazna_html($item['criteria']) ?></td>
<td data-label="Cijena">Na upit</td></tr>
<?php endforeach; ?></tbody></table></div></section>
<?php endif; ?>
<?php endif; ?>
<p class="bazna-small">Bazna cijena nije automatski jednaka najnižoj cijeni tijekom 30 dana prije sniženja. <a href="<?= bazna_html($core->link('bazna/v1/cjenici')) ?>">Javni BAZNA API</a></p>
</main><footer class="bazna-footer">BAZNA · Brendigo · brendigo.com</footer></body></html>
