<?php
/** PHP template included after authenticated session setup. */
declare(strict_types=1);
$authenticated = ($_SESSION['admin'] ?? false) === true;
$tabs = [
    'pregled' => 'Pregled', 'proizvodi' => 'Proizvodi', 'usluge' => 'Usluge',
    'poslovnice' => 'Poslovnice', 'uvoz-izvoz' => 'Uvoz/Izvoz',
    'javni-cjenici' => 'Javni cjenici', 'arhiva' => 'Arhiva',
    'postavke' => 'Postavke', 'licenca' => 'Licenca',
];
$tab = (string) ($_GET['tab'] ?? 'pregled');
if (!isset($tabs[$tab])) $tab = 'pregled';
$draft = $core->read('draft.json');
$published = $core->read('published.json');
$pretty = json_encode(array_intersect_key($draft, array_flip(['outlets','products','services'])),
    JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
$licenseActive = $license?->active() ?? false;
$csrf = (string) ($_SESSION['csrf'] ?? '');
$endpoint = $core->link('bazna/v1/cjenici');
header("Content-Security-Policy: default-src 'none'; style-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
?><!doctype html>
<html lang="hr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>BAZNA — Administracija</title><link rel="stylesheet" href="<?= bazna_html($core->link('bazna/assets/bazna.css')) ?>"></head>
<body class="bazna-admin">
<div class="bazna-shell">
<header class="bazna-topbar"><div><span class="bazna-mark">B</span> <strong>BAZNA</strong><small>Bazne cijene by Brendigo</small></div>
<a href="<?= bazna_html($core->link('bazna/cjenik')) ?>">Javni cjenik</a></header>
<?php if (!$authenticated): ?>
<main class="bazna-login"><section class="bazna-panel"><h1>Prijava u BAZNA</h1>
<p>Za uređivanje kataloga potrebna je administratorska prijava.</p>
<?php if ($message !== ''): ?><p role="status"><?= bazna_html($message) ?></p><?php endif; ?>
<form method="post" action="<?= bazna_html($core->link('bazna/admin')) ?>">
<input type="hidden" name="_csrf" value="<?= bazna_html($csrf) ?>">
<input type="hidden" name="action" value="login">
<label>Administratorska lozinka<input type="password" name="password" autocomplete="current-password" required></label>
<button type="submit">Prijava</button></form></section></main>
<?php else: ?>
<div class="bazna-workspace"><nav class="bazna-sidebar" aria-label="BAZNA izbornik">
<?php foreach ($tabs as $id => $label): ?>
<a <?= $id === $tab ? 'aria-current="page"' : '' ?> href="<?= bazna_html($core->link('bazna/admin?tab=' . $id)) ?>"><?= bazna_html($label) ?></a>
<?php endforeach; ?>
<form method="post"><input type="hidden" name="_csrf" value="<?= bazna_html($csrf) ?>">
<input type="hidden" name="action" value="logout"><button type="submit" class="bazna-secondary">Odjava</button></form>
</nav><main class="bazna-main"><div class="bazna-page-title"><div>
<p class="bazna-eyebrow">BAZNA / <?= bazna_html($tabs[$tab]) ?></p>
<h1><?= bazna_html($tabs[$tab]) ?></h1></div><span class="bazna-pill"><?= $licenseActive ? 'Licenca aktivna' : 'Licenca nije aktivna' ?></span></div>
<?php if ($message !== ''): ?><p role="status" class="bazna-message"><?= bazna_html($message) ?></p><?php endif; ?>
<?php if ($tab === 'pregled'): ?>
<div class="bazna-stats">
<div class="bazna-panel"><small>Proizvodi</small><strong><?= count($draft['products'] ?? []) ?></strong></div>
<div class="bazna-panel"><small>Usluge</small><strong><?= count($draft['services'] ?? []) ?></strong></div>
<div class="bazna-panel"><small>Poslovnice</small><strong><?= count($draft['outlets'] ?? []) ?></strong></div>
<div class="bazna-panel"><small>Javna objava</small><strong><?= isset($published['published_at']) ? 'Aktivna' : 'Na čekanju' ?></strong></div></div>
<section class="bazna-panel"><h2>Pravni okvir</h2>
<p>Početak primjene objavljenih odluka: <strong>17. 11. 2026.</strong></p>
<p>Povijesni referentni datumi: <strong>2. 5. 2025.</strong> i <strong>10. 9. 2026.</strong>, prema klasifikaciji.</p>
<p>Bazna cijena nije isto što i najniža cijena u 30 dana prije sniženja. Ne unosite povijesni iznos bez dokaza.</p></section>
<?php elseif (in_array($tab, ['proizvodi','usluge','poslovnice','uvoz-izvoz'], true)): ?>
<section class="bazna-panel"><h2>Uređivanje kataloga — <?= bazna_html($tabs[$tab]) ?></h2>
<p>Koristite strukturirani JSON kako biste sigurno uredili cijeli katalog, poslovnice, proizvode i usluge. Polje <code>historical_verified: true</code> smije se postaviti samo uz izvornu evidenciju za baznu cijenu. Pohrana je lokalna i ne šalje katalog licencnom servisu.</p>
<form method="post" action="<?= bazna_html($core->link('bazna/admin?tab=' . $tab)) ?>">
<input type="hidden" name="action" value="save"><input type="hidden" name="_csrf" value="<?= bazna_html($csrf) ?>">
<label>JSON nacrt kataloga
<textarea class="bazna-editor" name="catalog" spellcheck="false" required><?= bazna_html((string) $pretty) ?></textarea></label>
<button type="submit" <?= !$licenseActive ? 'disabled' : '' ?>>Spremi nacrt</button></form>
<p class="bazna-small">Nepoznata poslovnica, nepravilna cijena, datum ili nepotvrđena povijesna cijena odbijaju se prilikom spremanja.</p></section>
<?php elseif ($tab === 'javni-cjenici'): ?>
<section class="bazna-panel"><h2>Javni cjenici i BAZNA Android</h2>
<p>Veza za Android i sve ostale platforme: <code><?= bazna_html($endpoint) ?></code></p>
<p>Posljednja javna objava: <strong><?= bazna_html((string) ($published['published_at'] ?? 'nije objavljeno')) ?></strong></p>
<form method="post" action="<?= bazna_html($core->link('bazna/admin?tab=javni-cjenici')) ?>">
<input type="hidden" name="action" value="publish"><input type="hidden" name="_csrf" value="<?= bazna_html($csrf) ?>">
<button type="submit" <?= !$licenseActive ? 'disabled' : '' ?>>Objavi provjereni katalog</button></form>
<p>CSV: <a href="<?= bazna_html($core->link('bazna/v1/cjenici/proizvodi/csv')) ?>">Proizvodi</a> · <a href="<?= bazna_html($core->link('bazna/v1/cjenici/usluge/csv')) ?>">Usluge</a></p>
</section>
<?php elseif ($tab === 'arhiva'): ?>
<section class="bazna-panel"><h2>Arhiva objava</h2>
<p>Prethodne javne verzije čuvaju se bez automatskog brisanja povijesne evidencije.</p>
<?php foreach ($core->archives() as $entry): ?><p><a href="<?= bazna_html($entry['url']) ?>"><?= bazna_html($entry['date'] . ' — ' . $entry['kind']) ?> (CSV)</a></p><?php endforeach; ?></section>
<?php elseif ($tab === 'postavke'): ?>
<section class="bazna-panel"><h2>Postavke platforme</h2>
<p>Javna domena: <code><?= bazna_html($core->base()) ?></code></p>
<p>Podatkovni API v1: <code><?= bazna_html($endpoint) ?></code></p>
<p>Svi javni podaci potječu isključivo iz posljednje eksplicitne objave. Administratorska lozinka i licencni ključ pohranjeni su izvan javne mape.</p></section>
<?php elseif ($tab === 'licenca'): ?>
<section class="bazna-panel"><h2>BAZNA licenca</h2>
<p>Licenca vrijedi za jednu kanonsku domenu: <code><?= bazna_html($license?->domain() ?? $core->host()) ?></code>.</p>
<p>Stanje: <strong><?= $licenseActive ? 'aktivna' : 'nije aktivna' ?></strong>.</p>
<form method="post"><input type="hidden" name="_csrf" value="<?= bazna_html($csrf) ?>">
<label>Licencni ključ<input type="password" name="key" autocomplete="off" placeholder="BAZNA-00001-..." required></label>
<button name="action" value="activate" type="submit" <?= !$license ? 'disabled' : '' ?>>Aktiviraj licencu</button>
<button name="action" value="refresh" type="submit" <?= !$license ? 'disabled' : '' ?> class="bazna-secondary">Provjeri licencu</button>
<button name="action" value="deactivate" type="submit" <?= !$license ? 'disabled' : '' ?> class="bazna-secondary">Deaktiviraj licencu</button></form></section>
<?php endif; ?>
</main></div><?php endif; ?>
<footer class="bazna-footer">BAZNA · Brendigo · <a href="https://brendigo.com/">brendigo.com</a></footer></div></body></html>
