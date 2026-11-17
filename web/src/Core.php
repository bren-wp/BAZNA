<?php
/**
 * BAZNA platform-neutral catalog and publication engine.
 * PHP 8.1+, no WordPress/Joomla/Shopify dependency and no external SDK.
 * Private storage MUST live outside the publicly served document root.
 */
declare(strict_types=1);

namespace Brendigo\Bazna;

use InvalidArgumentException;
use RuntimeException;

final class Core
{
    public const VERSION = '1.1.5';
    public const FIELDS = [
        'id', 'naziv', 'vrsta', 'sku', 'marka', 'neto_kolicina', 'ean_barkod', 'jedinica_mjere',
        'cijena_jedinice_mjere', 'maloprodajna_cijena',
        'naziv_posebnog_oblika', 'sidrena_cijena', 'sidrena_datum',
        'barkod', 'dostupnost', 'slika_url',
    ];
    public const KINDS = ['proizvodi', 'usluge'];
    public const REFERENCE_DATES = ['legacy_fmcg' => '2025-05-02', 'general_goods_services' => '2026-09-10'];

    public function __construct(private readonly string $storage, private readonly string $baseUrl)
    {
        if (!preg_match('#^https://[a-z0-9.-]+(?::443)?(?:/[a-zA-Z0-9._~-]+)*/?$#Di', $baseUrl)) {
            throw new InvalidArgumentException('BAZNA_PUBLIC_URL mora biti javna HTTPS adresa bez upita.');
        }
        if (!str_starts_with($storage, '/') || str_contains($storage, '..')) {
            throw new InvalidArgumentException('BAZNA_PRIVATE_DIR mora biti apsolutna privatna putanja.');
        }
    }

    public function base(): string { return rtrim($this->baseUrl, '/'); }
    public function storage(): string { return $this->storage; }
    public function host(): string { return (string) parse_url($this->baseUrl, PHP_URL_HOST); }
    public function basePath(): string { return rtrim((string) parse_url($this->baseUrl, PHP_URL_PATH), '/') . '/'; }
    public function link(string $path): string { return $this->base() . '/' . ltrim($path, '/'); }

    public function ensurePrivate(): void
    {
        // Avoid realpath on a nonexistent path: enforce outside root at startup.
        $root = rtrim((string) ($_SERVER['DOCUMENT_ROOT'] ?? ''), '/');
        $data = rtrim($this->storage, '/');
        if ($root !== '' && ($data === $root || str_starts_with($data, $root . '/'))) {
            throw new RuntimeException('BAZNA_PRIVATE_DIR ne smije biti unutar DOCUMENT_ROOT.');
        }
        if (!is_dir($this->storage) && !mkdir($this->storage, 0700, true) && !is_dir($this->storage)) {
            throw new RuntimeException('Privatna mapa nije dostupna.');
        }
        $realRoot = $root === '' ? false : realpath($root);
        $realData = realpath($this->storage);
        if ($realRoot !== false && $realData !== false &&
            ($realData === $realRoot || str_starts_with($realData, rtrim($realRoot, '/') . '/'))) {
            throw new RuntimeException('BAZNA_PRIVATE_DIR vodi u javni DocumentRoot.');
        }
        if (!is_writable($this->storage)) {
            throw new RuntimeException('Privatna mapa nema dozvolu pisanja.');
        }
    }

    public static function validateItem(array $input): array
    {
        $row = [];
        foreach (self::FIELDS as $field) {
            $value = $input[$field] ?? '';
            if (!is_string($value) && !is_int($value) && !is_float($value)) {
                throw new InvalidArgumentException('Neispravan stupac: ' . $field);
            }
            $value = trim((string) $value);
            if (strlen($value) > 800 || preg_match('/[\x00-\x08\x0B\x0C\x0E-\x1F]/', $value)) {
                throw new InvalidArgumentException('Predugačak ili neispravan podatak: ' . $field);
            }
            $row[$field] = $value;
        }
        if ($row['naziv'] === '' || strlen($row['naziv']) > 240 || $row['maloprodajna_cijena'] === '') {
            throw new InvalidArgumentException('Naziv i aktualna maloprodajna cijena su obvezni.');
        }
        foreach (['maloprodajna_cijena', 'sidrena_cijena', 'cijena_jedinice_mjere'] as $money) {
            if ($row[$money] !== '' && !preg_match('/^\d{1,9}(?:[,.]\d{1,4})?$/D', $row[$money])) {
                throw new InvalidArgumentException('Neispravan iznos: ' . $money);
            }
        }
        if ($row['sidrena_cijena'] !== '') {
            $date = $row['sidrena_datum'];
            $validDate = preg_match('/^(\d{4})-(\d{2})-(\d{2})$/D', $date, $m) &&
                checkdate((int) $m[2], (int) $m[3], (int) $m[1]);
            if (!$validDate || ($input['historical_verified'] ?? false) !== true) {
                throw new InvalidArgumentException('Bazna cijena zahtijeva stvarno potvrđen povijesni iznos i datum.');
            }
        }
        if ($row['ean_barkod'] !== '' && !self::validEan($row['ean_barkod'])) {
            throw new InvalidArgumentException('EAN-8/EAN-13 kontrolna znamenka nije valjana.');
        }
        if ($row['neto_kolicina'] !== '' &&
            !preg_match('/^\\d{1,9}(?:[,.]\\d{1,5})?$/D', $row['neto_kolicina'])) {
            throw new InvalidArgumentException('Neto količina nije valjana.');
        }
        if ($row['slika_url'] !== '') {
            $urlHost = parse_url($row['slika_url'], PHP_URL_HOST);
            if (parse_url($row['slika_url'], PHP_URL_SCHEME) !== 'https' || !$urlHost) {
                throw new InvalidArgumentException('Fotografija mora imati HTTPS URL.');
            }
        }
        $row['historical_verified'] = ($input['historical_verified'] ?? false) === true;
        $outlet = $input['outlet'] ?? '';
        if (!is_string($outlet) || !preg_match('/^[a-zA-Z0-9-]{0,64}$/D', $outlet)) {
            throw new InvalidArgumentException('Oznaka poslovnice nije valjana.');
        }
        $row['outlet'] = $outlet;
        return $row;
    }

    /**
     * Services with individually agreed prices: descriptive criteria are
     * public only after explicit publication, never numeric CSV/XML entries.
     */
    public static function validateInquiry(array $input): array
    {
        foreach (['id', 'naziv', 'kriteriji', 'outlet'] as $field) {
            if (isset($input[$field]) && !is_string($input[$field])) {
                throw new InvalidArgumentException('Neispravno polje usluge na upit.');
            }
        }
        $id = trim($input['id'] ?? '');
        $name = trim($input['naziv'] ?? '');
        $criteria = trim($input['kriteriji'] ?? '');
        $outlet = trim($input['outlet'] ?? '');
        if (!preg_match('/^[A-Za-z0-9-]{1,64}$/D', $id) ||
            $name === '' || strlen($name) > 240 || $criteria === '' ||
            strlen($criteria) > 800 ||
            !preg_match('/^[A-Za-z0-9-]{0,64}$/D', $outlet) ||
            preg_match('/[\\x00-\\x1F]/', $name . $criteria)) {
            throw new InvalidArgumentException('Usluga na upit treba naziv, kriterije i valjanu poslovnicu.');
        }
        return ['id' => $id, 'naziv' => $name, 'kriteriji' => $criteria, 'outlet' => $outlet];
    }

    public static function validateCatalog(array $document): array
    {
        $outlets = $document['outlets'] ?? [];
        $products = $document['products'] ?? [];
        $services = $document['services'] ?? [];
        $inquiries = $document['on_request'] ?? [];
        if (!is_array($outlets) || !array_is_list($outlets) || count($outlets) > 500 ||
            !is_array($products) || !array_is_list($products) || count($products) > 10000 ||
            !is_array($services) || !array_is_list($services) || count($services) > 10000 ||
            !is_array($inquiries) || !array_is_list($inquiries) || count($inquiries) > 500) {
            throw new InvalidArgumentException('Neispravna veličina kataloga ili poslovnica.');
        }
        $seen = [];
        $cleanOutlets = [];
        foreach ($outlets as $outlet) {
            if (!is_array($outlet)) throw new InvalidArgumentException('Neispravna poslovnica.');
            $code = trim((string) ($outlet['code'] ?? ''));
            $name = trim((string) ($outlet['name'] ?? ''));
            $address = trim((string) ($outlet['address'] ?? ''));
            $type = trim((string) ($outlet['type'] ?? ''));
            if (!preg_match('/^[A-Za-z0-9][A-Za-z0-9-]{0,63}$/D', $code) ||
                isset($seen[strtolower($code)]) || $name === '' || strlen($name) > 240 ||
                strlen($address) > 400 || strlen($type) > 100 ||
                preg_match('/[\x00-\x1F]/', $address . $type)) {
                throw new InvalidArgumentException('Oznaka, naziv ili adresa poslovnice nisu valjani.');
            }
            $seen[strtolower($code)] = true;
            $outletClean = ['code' => $code, 'name' => $name, 'address' => $address];
            if ($type !== '') $outletClean['type'] = $type;
            $cleanOutlets[] = $outletClean;
        }
        $result = ['outlets' => $cleanOutlets, 'products' => [], 'services' => [], 'on_request' => []];
        foreach (['products', 'services'] as $collection) {
            foreach ($document[$collection] ?? [] as $input) {
                if (!is_array($input)) throw new InvalidArgumentException('Neispravna stavka.');
                $row = self::validateItem($input);
                if ($row['outlet'] !== '' && !isset($seen[strtolower($row['outlet'])])) {
                    throw new InvalidArgumentException('Stavka je pridružena nepostojećoj poslovnici.');
                }
                $row['vrsta'] = $collection === 'products' ? 'proizvod' : 'usluga';
                $result[$collection][] = $row;
            }
        }
        $seenInquiry = [];
        foreach ($inquiries as $input) {
            if (!is_array($input)) throw new InvalidArgumentException('Neispravna usluga na upit.');
            $row = self::validateInquiry($input);
            if (isset($seenInquiry[$row['id']]) ||
                ($row['outlet'] !== '' && !isset($seen[strtolower($row['outlet'])]))) {
                throw new InvalidArgumentException('Duplikat usluge na upit ili nepoznata poslovnica.');
            }
            $seenInquiry[$row['id']] = true;
            $result['on_request'][] = $row;
        }
        return $result;
    }

    private function file(string $name): string
    {
        if (!preg_match('/^[a-z0-9-]+\.json$/D', $name)) throw new InvalidArgumentException('Neispravna datoteka.');
        return rtrim($this->storage, '/') . '/' . $name;
    }

    public function read(string $file): array
    {
        $path = $this->file($file);
        if (!is_file($path)) return ['outlets' => [], 'products' => [], 'services' => [], 'on_request' => []];
        if (filesize($path) > 8 * 1024 * 1024) throw new RuntimeException('Katalog prelazi dopuštenu veličinu.');
        $value = json_decode((string) file_get_contents($path), true, 40, JSON_THROW_ON_ERROR);
        if (!is_array($value)) throw new RuntimeException('Neispravna spremljena datoteka.');
        return $value;
    }

    public function write(string $file, array $data): void
    {
        $this->ensurePrivate();
        $path = $this->file($file);
        $lock = fopen($path . '.lock', 'c');
        if (!$lock || !flock($lock, LOCK_EX)) throw new RuntimeException('Pohrana je zauzeta.');
        try {
            $temp = tempnam($this->storage, '.bazna-');
            if ($temp === false) throw new RuntimeException('Nema mjesta za pohranu.');
            try {
                $bytes = json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR);
                if (strlen($bytes) > 8 * 1024 * 1024 || file_put_contents($temp, $bytes, LOCK_EX) !== strlen($bytes)) {
                    throw new RuntimeException('Pohrana nije uspjela.');
                }
                chmod($temp, 0600);
                if (!rename($temp, $path)) throw new RuntimeException('Zamjena pohrane nije uspjela.');
            } finally {
                if (is_file($temp)) unlink($temp);
            }
        } finally {
            flock($lock, LOCK_UN);
            fclose($lock);
        }
    }

    /**
     * One cross-process lock serializes the WHOLE catalog transaction.
     * Per-file atomic renames alone do not protect concurrent read/modify/write
     * operations or preserve previous prices while two admins publish.
     */
    private function catalogTransaction(callable $action): mixed
    {
        $this->ensurePrivate();
        $handle = fopen(rtrim($this->storage, '/') . '/catalog-transaction.lock', 'c');
        if ($handle === false || !flock($handle, LOCK_EX)) {
            throw new RuntimeException('Nije moguće zaključati BAZNA katalog.');
        }
        try {
            return $action();
        } finally {
            flock($handle, LOCK_UN);
            fclose($handle);
        }
    }

    public function saveDraft(array $document): void
    {
        $validated = self::validateCatalog($document);
        $this->catalogTransaction(function () use ($validated): void {
            $this->write('draft.json', $validated);
        });
    }

    /**
     * Update a single item from the same UI used by standalone PHP and Joomla.
     * Reject a stale form rather than overwriting another admin's changes.
     * $index is -1 for a new row; otherwise a zero-based draft row index.
     */
    public function modifyRow(string $collection, int $index, ?array $row, string $expectedHash = ''): void
    {
        if (!in_array($collection, ['products', 'services', 'outlets', 'on_request'], true) ||
            $index < -1 || $index > 10000) {
            throw new InvalidArgumentException('Nevaljana zbirka ili indeks retka.');
        }
        $this->catalogTransaction(function () use ($collection, $index, $row, $expectedHash): void {
            $draft = $this->read('draft.json');
            $rows = $draft[$collection] ?? [];
            if (!is_array($rows) || !array_is_list($rows)) {
                throw new RuntimeException('Neispravan zapis kataloga.');
            }
            if ($index === -1) {
                if ($row === null) throw new InvalidArgumentException('Nema retka za brisanje.');
                if ($expectedHash !== '') throw new InvalidArgumentException('Nevaljan novi redak.');
                if ($collection !== 'outlets' && trim((string) ($row['id'] ?? '')) === '') {
                    $row['id'] = 'bazna-' . bin2hex(random_bytes(8));
                }
                $rows[] = $row;
            } else {
                if (!isset($rows[$index]) || !is_array($rows[$index]) ||
                    !preg_match('/^[a-f0-9]{64}$/D', $expectedHash)) {
                    throw new InvalidArgumentException('Redak više ne postoji ili zahtjev nema verziju.');
                }
                $actualHash = hash('sha256', json_encode($rows[$index],
                    JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR));
                if (!hash_equals($actualHash, $expectedHash)) {
                    throw new InvalidArgumentException('Podaci su izmijenjeni u drugoj sesiji. Osvježite stranicu.');
                }
                if ($row === null) {
                    array_splice($rows, $index, 1);
                } else {
                    // IDs and outlet codes are stable references. Renaming an
                    // outlet requires an explicit migration, not an implicit
                    // deletion of its assigned products and archived links.
                    $identity = $collection === 'outlets' ? 'code' : 'id';
                    if ((string) ($rows[$index][$identity] ?? '') !==
                        (string) ($row[$identity] ?? '')) {
                        throw new InvalidArgumentException('Identifikator postojećeg retka nije moguće mijenjati.');
                    }
                    $rows[$index] = $row;
                }
            }
            $draft[$collection] = $rows;
            $validated = self::validateCatalog($draft);
            if ($collection !== 'outlets') {
                $seen = [];
                foreach ($validated[$collection] as $item) {
                    $id = (string) ($item['id'] ?? '');
                    if ($id === '' || isset($seen[$id])) {
                        throw new InvalidArgumentException('Šifra stavke mora biti jedinstvena.');
                    }
                    $seen[$id] = true;
                }
            }
            $this->write('draft.json', $validated);
        });
    }

    /**
     * Atomic publication. Auto mode is idempotent and refuses an incomplete
     * official catalog; manual publication stays available for legacy clients.
     * No website traffic or WP-Cron is needed when a server scheduler invokes
     * bin/publish.php periodically.
     */
    public function publish(bool $onlyWhenChanged = false, bool $requireOfficial = false): array
    {
        return $this->catalogTransaction(function () use ($onlyWhenChanged, $requireOfficial): array {
            $catalog = self::validateCatalog($this->read('draft.json'));
            $existing = $this->read('published.json');
            $identity = $this->officialIdentity();
            if ($requireOfficial) {
                if ($identity === '') {
                    throw new RuntimeException('Automatska objava nije moguća bez valjanog OIB-a ili matičnog broja.');
                }
                foreach (array_merge([''], array_column($catalog['outlets'], 'code')) as $outlet) {
                    foreach (self::KINDS as $kind) {
                        $missing = $this->officialMissing($kind, $outlet, $catalog);
                        if ($missing !== []) {
                            $first = $missing[0];
                            throw new RuntimeException(
                                'Automatska objava za ' . $kind . ' / ' .
                                ($outlet !== '' ? $outlet : 'webshop') .
                                ': nedostaje ' . $first['field'] . ' u retku ' . $first['row'] . '.'
                            );
                        }
                        // The filename also needs the exact shop/outlet type,
                        // registered address and code; never invent them.
                        $candidate = $catalog;
                        $candidate['published_at'] = gmdate('c');
                        $candidate['official_identity'] = $identity;
                        $this->officialFilename($kind, 'csv', $outlet, $candidate);
                    }
                }
            }
            // Never publish duplicates or modify the archive on an unchanged
            // scheduled run, including when verified identity remains stable.
            if ($onlyWhenChanged && isset($existing['published_at']) &&
                array_intersect_key($existing, $catalog) === $catalog &&
                ($existing['official_identity'] ?? '') === $identity) {
                return $existing;
            }
            $timestamp = gmdate('c');
            // Retain archives indefinitely until merchant-supervised legal
            // retention review. Prevent two concurrent publishes from losing
            // one archive-index update.
            if (isset($existing['published_at'])) {
                $history = $this->read('archive.json');
                $versions = $history['versions'] ?? [];
                $archiveId = gmdate('YmdHis') . '-' . bin2hex(random_bytes(6));
                $archiveFile = 'archive-' . $archiveId . '.json';
                $this->write($archiveFile, $existing);
                array_unshift($versions, [
                    'id' => $archiveId, 'file' => $archiveFile,
                    'published_at' => $existing['published_at'],
                ]);
                $history['versions'] = $versions;
                $this->write('archive.json', $history);
            }
            $catalog['published_at'] = $timestamp;
            // Monotonic per-store publication sequence, retained with each snapshot.
            $catalog['publication_sequence'] = max(1, (int) ($existing['publication_sequence'] ?? 0) + 1);
            $catalog['official_identity'] = $identity;
            $this->write('published.json', $catalog);
            return $catalog;
        });
    }

    /** Published snapshot only. Never serves draft inquiry criteria. */
    public function onRequestServices(string $outlet = '', string $query = ''): array
    {
        if (!preg_match('/^[a-zA-Z0-9-]{0,64}$/D', $outlet) ||
            strlen($query) > 100 || preg_match('/[\\x00-\\x1F]/', $query)) {
            throw new InvalidArgumentException('Neispravan javni upit usluge.');
        }
        $terms = preg_split('/\\s+/u', trim($query), -1, PREG_SPLIT_NO_EMPTY);
        if ($terms === false) throw new InvalidArgumentException('Neispravna pretraga.');
        $published = $this->read('published.json');
        $items = [];
        foreach ($published['on_request'] ?? [] as $row) {
            if ($outlet !== '' && ($row['outlet'] ?? '') !== $outlet) continue;
            $haystack = (string) ($row['naziv'] ?? '') . ' ' .
                (string) ($row['kriteriji'] ?? '');
            $matches = true;
            foreach ($terms as $term) {
                if (function_exists('mb_stripos')
                    ? mb_stripos($haystack, $term, 0, 'UTF-8') === false
                    : stripos($haystack, $term) === false) {
                    $matches = false;
                    break;
                }
            }
            if (!$matches) continue;
            $items[] = [
                'id' => $row['id'], 'name' => $row['naziv'],
                'criteria' => $row['kriteriji'], 'outlet' => $row['outlet'],
            ];
        }
        return ['schema' => 1, 'items' => $items];
    }

    /**
     * Reuse one published-only HTML table on Web PHP, Joomla and Shopify.
     * Never read draft.json, never infer an unpublished historical price.
     * Search covers both numeric services and price-on-request criteria.
     */
    public function publicCatalogHtml(string $kind, string $outlet = '', string $query = ''): string
    {
        if (!in_array($kind, self::KINDS, true) ||
            !preg_match('/^[A-Za-z0-9-]{0,64}$/D', $outlet) ||
            strlen($query) > 256 || preg_match('/[\\x00-\\x1F]/', $query)) {
            throw new InvalidArgumentException('Neispravan javni upit cjenika.');
        }
        $published = $this->read('published.json');
        if (!isset($published['published_at'])) {
            return '<p class="bazna-muted">Javni cjenik još nije objavljen.</p>';
        }
        $terms = preg_split('/\\s+/u', trim($query), -1, PREG_SPLIT_NO_EMPTY);
        if ($terms === false) throw new InvalidArgumentException('Neispravna pretraga.');
        $matches = static function (string $haystack) use ($terms): bool {
            foreach ($terms as $term) {
                if (function_exists('mb_stripos')) {
                    if (mb_stripos($haystack, $term, 0, 'UTF-8') === false) return false;
                } elseif (stripos($haystack, $term) === false) {
                    return false;
                }
            }
            return true;
        };
        $rows = $published[$kind === 'proizvodi' ? 'products' : 'services'] ?? [];
        $html = '<section class="bazna-panel bazna-table-scroll"><table><thead><tr>' .
            '<th>Naziv</th><th>SKU</th><th>Redovna cijena</th><th>Bazna cijena</th>' .
            '<th>Referentni datum</th><th>Jedinica mjere</th><th>Dostupnost</th>' .
            '</tr></thead><tbody>';
        $count = 0;
        foreach ($rows as $row) {
            if (!is_array($row) || ($row['outlet'] ?? '') !== $outlet ||
                !$matches(implode(' ', [
                    (string) ($row['naziv'] ?? ''), (string) ($row['sku'] ?? ''),
                    (string) ($row['marka'] ?? ''), (string) ($row['barkod'] ?? ''),
                ]))) {
                continue;
            }
            $fields = ['naziv', 'sku', 'maloprodajna_cijena', 'sidrena_cijena',
                'sidrena_datum', 'jedinica_mjere', 'dostupnost'];
            $html .= '<tr>';
            foreach ($fields as $field) {
                $raw = (string) ($row[$field] ?? '');
                $value = $raw === '' ? '—' : $raw;
                if ($raw !== '' && in_array($field, ['maloprodajna_cijena', 'sidrena_cijena'], true)) {
                    $value .= ' €';
                }
                $html .= '<td data-label="' . self::esc([
                    'naziv' => 'Naziv', 'sku' => 'SKU', 'maloprodajna_cijena' => 'Redovna cijena',
                    'sidrena_cijena' => 'Bazna cijena', 'sidrena_datum' => 'Referentni datum',
                    'jedinica_mjere' => 'Jedinica mjere', 'dostupnost' => 'Dostupnost',
                ][$field]) . '">' . self::esc($value) . '</td>';
            }
            $html .= '</tr>';
            ++$count;
        }
        $html .= '</tbody></table>';
        if ($count === 0) $html .= '<p class="bazna-muted">Nema stavki za odabranu pretragu i poslovnicu.</p>';
        $html .= '</section>';
        if ($kind === 'usluge') {
            $html .= '<section class="bazna-panel"><h2>Cijena na upit</h2>' .
                '<p class="bazna-muted">Iznos se određuje prema objavljenim kriterijima; ' .
                'ne dodaje se izmišljena brojčana cijena u CSV/XML.</p>' .
                '<div class="bazna-table-scroll"><table><thead><tr>' .
                '<th>Usluga</th><th>Kriteriji</th><th>Cijena</th></tr></thead><tbody>';
            $inquiryCount = 0;
            foreach ($published['on_request'] ?? [] as $row) {
                if (!is_array($row) || ($row['outlet'] ?? '') !== $outlet ||
                    !$matches((string) ($row['naziv'] ?? '') . ' ' . (string) ($row['kriteriji'] ?? ''))) {
                    continue;
                }
                $html .= '<tr><td data-label="Usluga">' . self::esc((string) ($row['naziv'] ?? '')) .
                    '</td><td data-label="Kriteriji">' . self::esc((string) ($row['kriteriji'] ?? '')) .
                    '</td><td data-label="Cijena">Na upit</td></tr>';
                ++$inquiryCount;
            }
            $html .= '</tbody></table></div>';
            if ($inquiryCount === 0) {
                $html .= '<p class="bazna-muted">Nema objavljenih usluga na upit za odabrane uvjete.</p>';
            }
            $html .= '</section>';
        }
        return $html;
    }

    /** NN 132/2026 article 3: exact public ordering, separate from legacy Android v1 CSV. */
    public const OFFICIAL_FIELDS = [
        'proizvodi' => [
            'naziv', 'sku', 'marka', 'neto_kolicina', 'jedinica_mjere',
            'maloprodajna_cijena', 'poseban_oblik_prodaje',
            'cijena_jedinice_mjere', 'barkod', 'ean_barkod', 'bazna_cijena',
        ],
        'usluge' => ['naziv', 'maloprodajna_cijena', 'poseban_oblik_prodaje', 'bazna_cijena'],
    ];

    public static function validOib(string $oib): bool
    {
        if (!preg_match('/^\d{11}$/D', $oib)) return false;
        $state = 10;
        for ($i = 0; $i < 10; $i++) {
            $state = ($state + (int) $oib[$i]) % 10;
            if ($state === 0) $state = 10;
            $state = ($state * 2) % 11;
        }
        $check = 11 - $state;
        return ($check === 10 ? 0 : $check) === (int) $oib[10];
    }

    public static function validEan(string $value): bool
    {
        if (!preg_match('/^(?:\d{8}|\d{13})$/D', $value)) return false;
        $n = strlen($value) - 1;
        $sum = 0;
        for ($i = 0; $i < $n; $i++) $sum += (int) $value[$i] * (($n - $i) % 2 ? 3 : 1);
        return (10 - $sum % 10) % 10 === (int) $value[$n];
    }

    /** Must be explicitly configured. An unknown identity cannot be invented. */
    public function officialIdentity(): string
    {
        $oib = (string) (getenv('BAZNA_TRADER_OIB') ?: '');
        $mb = (string) (getenv('BAZNA_TRADER_MB') ?: '');
        if ($oib !== '' && $mb !== '') return '';
        if ($oib !== '') return self::validOib($oib) ? $oib : '';
        return preg_match('/^\d{8}$/D', $mb) ? $mb : '';
    }

    /** Preview missing historical data, never silently certify a catalog. */
    public function officialMissing(string $kind, string $outlet = '', ?array $snapshot = null): array
    {
        if (!isset(self::OFFICIAL_FIELDS[$kind]) || ($outlet !== '' &&
            !preg_match('/^[A-Za-z0-9][A-Za-z0-9-]{0,63}$/D', $outlet))) {
            throw new InvalidArgumentException('Nepodržana vrsta ili poslovnica.');
        }
        $snapshot ??= $this->read('published.json');
        $rows = $snapshot[$kind === 'proizvodi' ? 'products' : 'services'] ?? [];
        $issues = [];
        foreach ($rows as $index => $row) {
            if (($row['outlet'] ?? '') !== $outlet) continue;
            foreach (['naziv', 'maloprodajna_cijena', 'sidrena_cijena'] as $field) {
                if (trim((string) ($row[$field] ?? '')) === '') {
                    $issues[] = ['row' => $index + 1, 'field' => $field];
                }
            }
            if (!empty($row['cijena_jedinice_mjere']) && $kind === 'proizvodi') {
                foreach (['jedinica_mjere', 'neto_kolicina'] as $field) {
                    if (trim((string) ($row[$field] ?? '')) === '') {
                        $issues[] = ['row' => $index + 1, 'field' => $field];
                    }
                }
            }
        }
        return $issues;
    }

    /** Strict official projection; never publishes a draft, failed evidence or fake history. */
    public function officialRows(string $kind, string $outlet = '', ?array $snapshot = null): array
    {
        $snapshot ??= $this->read('published.json');
        if (!isset($snapshot['published_at']) ||
            (($snapshot['official_identity'] ?? $this->officialIdentity()) === '')) {
            throw new \RuntimeException('Službeni cjenik zahtijeva objavljene podatke i valjan OIB/MB trgovca.');
        }
        if ($this->officialMissing($kind, $outlet, $snapshot) !== []) {
            throw new \RuntimeException('Službeni cjenik ima neprovjerene ili nepotpune obvezne podatke.');
        }
        $rows = [];
        foreach ($snapshot[$kind === 'proizvodi' ? 'products' : 'services'] ?? [] as $row) {
            if (($row['outlet'] ?? '') !== $outlet) continue;
            $values = [];
            foreach (self::OFFICIAL_FIELDS[$kind] as $field) {
                $values[$field] = match ($field) {
                    'bazna_cijena' => (string) ($row['sidrena_cijena'] ?? ''),
                    'poseban_oblik_prodaje' => !empty($row['naziv_posebnog_oblika']) ? 'da' : 'ne',
                    default => (string) ($row[$field] ?? ''),
                };
            }
            $rows[] = $values;
        }
        return $rows;
    }

    /** Stable URL; Content-Disposition name includes identity, outlet, type and local timestamp. */
    public function officialFilename(string $kind, string $format, string $outlet = '', ?array $snapshot = null): string
    {
        if (!isset(self::OFFICIAL_FIELDS[$kind]) || !in_array($format, ['csv','xml'], true)) {
            throw new InvalidArgumentException('Neispravna vrsta cjenika.');
        }
        $published = $snapshot ?? $this->read('published.json');
        $id = (string) ($published['official_identity'] ?? $this->officialIdentity());
        if ($id === '') throw new \RuntimeException('Konfigurirajte OIB ili matični broj.');
        $date = (new \DateTimeImmutable((string) ($published['published_at'] ?? 'now')))
            ->setTimezone(new \DateTimeZone('Europe/Zagreb'))->format('Ymd_Hi');
        if ($outlet !== '' && !preg_match('/^[A-Za-z0-9][A-Za-z0-9-]{0,63}$/D', $outlet)) {
            throw new InvalidArgumentException('Nevaljana oznaka poslovnice.');
        }
        $place = 'ZA_SVA_PRODAJNA_MJESTA';
        $outlets = $published['outlets'] ?? [];
        if ($outlet !== '' || $outlets !== []) {
            $match = null;
            if ($outlet !== '') {
                foreach ($outlets as $row) {
                    if (($row['code'] ?? '') === $outlet) {
                        $match = $row;
                        break;
                    }
                }
                if ($match === null) throw new \RuntimeException('Poslovnica nije u objavljenom katalogu.');
                $kindOfPlace = (string) ($match['type'] ?? '');
                $address = (string) ($match['address'] ?? '');
                $code = $outlet;
            }
            else {
                // A webshop feed is NOT automatically the same price list as
                // all physical outlets; operator supplies exact place metadata.
                $kindOfPlace = (string) (getenv('BAZNA_MAIN_STORE_TYPE') ?: '');
                $address = (string) (getenv('BAZNA_MAIN_STORE_ADDRESS') ?: '');
                $code = (string) (getenv('BAZNA_MAIN_STORE_CODE') ?: '');
            }
            $slug = static function (string $raw): string {
                $raw = strtolower(trim($raw));
                $safe = preg_replace('/[^a-z0-9-]+/', '-', $raw);
                return trim((string) $safe, '-');
            };
            $typeName = $slug($kindOfPlace);
            $street = $slug($address);
            $branch = $slug($code);
            if ($typeName === '' || $street === '' || $branch === '') {
                throw new \RuntimeException('Nedostaju pravni tip prodajnog mjesta, adresa ili oznaka.');
            }
            $place = $typeName . '_' . $street . '_' . $branch;
        }
        $type = $kind === 'proizvodi' ? 'ROBA' : 'USLUGE';
        $sequence = max(1, (int) ($published['publication_sequence'] ?? 1));
        return $id . '_' . $place . '_' . $type . '_' . $sequence . '_' . $date . '.' . $format;
    }

    public function officialCsv(string $kind, string $outlet = '', ?array $snapshot = null): string
    {
        $rows = $this->officialRows($kind, $outlet, $snapshot);
        $f = fopen('php://temp', 'w+');
        if (!$f) throw new \RuntimeException('CSV nije dostupan.');
        fwrite($f, "\xEF\xBB\xBF");
        fputcsv($f, self::OFFICIAL_FIELDS[$kind], ';', '"', '');
        foreach ($rows as $row) {
            $values = array_values($row);
            foreach ($values as &$value) {
                if (preg_match('/^[\s]*[=+\-@]/u', (string) $value)) $value = "'" . $value;
            }
            unset($value);
            fputcsv($f, $values, ';', '"', '');
        }
        rewind($f);
        $content = (string) stream_get_contents($f);
        fclose($f);
        return $content;
    }

    public function officialXml(string $kind, string $outlet = '', ?array $snapshot = null): string
    {
        $rows = $this->officialRows($kind, $outlet, $snapshot);
        $dom = new \DOMDocument('1.0', 'UTF-8');
        $dom->formatOutput = true;
        $root = $dom->createElement('cjenik');
        $dom->appendChild($root);
        foreach ($rows as $row) {
            $entry = $dom->createElement($kind === 'proizvodi' ? 'roba' : 'usluga');
            foreach ($row as $key => $value) {
                $element = $dom->createElement($key);
                $element->appendChild($dom->createTextNode($value));
                $entry->appendChild($element);
            }
            $root->appendChild($entry);
        }
        return (string) $dom->saveXML();
    }

    public function csv(string $kind, string $outlet = '', ?array $snapshot = null): string
    {
        if (!in_array($kind, self::KINDS, true) || ($outlet !== '' &&
            !preg_match('/^[A-Za-z0-9][A-Za-z0-9-]{0,63}$/D', $outlet))) {
            throw new InvalidArgumentException('Nepodržana vrsta ili poslovnica.');
        }
        $published = $snapshot ?? $this->read('published.json');
        $rows = $published[$kind === 'proizvodi' ? 'products' : 'services'] ?? [];
        $handle = fopen('php://temp', 'w+');
        if (!$handle) throw new RuntimeException('CSV nije moguće pripremiti.');
        fwrite($handle, "\xEF\xBB\xBF");
        fputcsv($handle, self::FIELDS, ';', '"', '');
        foreach ($rows as $row) {
            if (($row['outlet'] ?? '') !== $outlet) continue;
            fputcsv($handle, array_map(static fn($field) => $row[$field] ?? '', self::FIELDS), ';', '"', '');
        }
        rewind($handle);
        $csv = stream_get_contents($handle);
        fclose($handle);
        return (string) $csv;
    }

    public function xml(string $kind, string $outlet = ''): string
    {
        $csv = $this->csv($kind, $outlet);
        $doc = new \DOMDocument('1.0', 'UTF-8');
        $doc->formatOutput = true;
        $root = $doc->createElement('bazna-cjenik');
        $root->setAttribute('vrsta', $kind);
        $doc->appendChild($root);
        $handle = fopen('php://temp', 'w+');
        fwrite($handle, $csv);
        rewind($handle);
        $headers = fgetcsv($handle, 0, ';', '"', '');
        // CSV uses a UTF-8 BOM for spreadsheet compatibility. It is not a
        // valid first character of an XML element name.
        if (is_array($headers) && isset($headers[0])) {
            $headers[0] = preg_replace('/^\xEF\xBB\xBF/', '', $headers[0]);
        }
        while (($values = fgetcsv($handle, 0, ';', '"', '')) !== false) {
            if (count($values) !== count($headers)) continue;
            $entry = $doc->createElement('stavka');
            foreach ($headers as $i => $name) {
                $field = $doc->createElement($name);
                $field->appendChild($doc->createTextNode((string) $values[$i]));
                $entry->appendChild($field);
            }
            $root->appendChild($entry);
        }
        fclose($handle);
        return (string) $doc->saveXML();
    }

    /**
     * Cross-platform legal metadata aligned with the existing WordPress v1
     * contract. This is NOT certification of the merchant's historical data.
     * Proposed rulebooks may change before final publication.
     */
    public function legal(): array
    {
        // Kept byte-for-byte equivalent to the WordPress v1 public legal fields.
        // Test web/tests/test.php enforces full cross-platform parity.
        return array(
                    'schema' => 1,
                    'reviewed_on' => '2026-11-17',
                    'jurisdiction' => 'HR',
                    'rules_revision' => 'NN-132-2026-1583-1584',
                    'consumer_law_revision' => 'NN-59-2026',
                    'unit_price_rule_revision' => 'NN-105-2026',
                    'unit_price_rule_effective_from' => '2026-09-26',
                    'price_reduction_lookback_days' => 30,
                    'discount_baseline_is_anchor_price' => false,
                    // Enacted under NN 132/2026; actual merchant price evidence is NOT certified.
                    'base_price_regulation_status' => 'enacted',
                    'base_price_rule_effective_from' => '2026-11-17',
                    'base_price_is_automatically_derived' => false,
                    'historical_price_verification_required' => true,
                    'legal_compliance_certified' => false,
                    'consumer_30_day_low_is_anchor_price' => false,
                    'consumer_30_day_low_is_base_price' => false,
                    'draft_rulebooks_consultation_until' => '2026-11-13',
                    'final_rulebooks_verified' => true,
                    // The enacted legal rules are active; catalog compliance still requires verification.
                    'implementation' => array(
                        'published_decisions_ready_from' => '2026-11-17',
                        'reference_price_data_entry' => 'verified_manual',
                        'product_service_publication' => 'csv_xml_html',
                        'publication_source_of_truth' => 'wordPress_published_feeds',
                        'new_rules_effective' => true,
                        'unpublished_draft_features' => 'not_automatically_enabled',
                        'discount_30_day_low_independent' => true,
                        'automatic_historical_inference' => false,
                    ),
                    'effective_from' => '2026-11-17',
                    'reference_dates' => array(
                        'legacy_fmcg' => '2025-05-02',
                        'fmcg' => '2025-05-02',
                        'general_goods_services' => '2026-09-10',
                    ),
                    'legacy_categories' => array(
                        'hrana', 'pice', 'kozmetika', 'sredstva_za_ciscenje',
                        'toaletne_potrepstine', 'proizvodi_za_kucanstvo',
                    ),
                    'product_publication_deadline' => '11:00',
                    'service_change_deadline' => '11:00',
                    'timezone' => 'Europe/Zagreb', // Croatian regulatory deadline, regardless of WordPress site timezone.
                    'archive_min_days' => 15,
                    'archive_retention_anchor' => 'replacement_time',
                    'price_list_update_deadline_local' => '11:00',
                    'advertising_print_tv_base_price_required' => true,
                    'reference_fmcg_discount_method' => 'lowest_price_in_30_days_before_discount_start',
                    'reference_general_discount_method' => 'previous_retail_price_before_discount',
                    'formats' => array( 'csv', 'xml' ),
                    'products_required_fields' => array(
                        'naziv', 'sku', 'marka', 'neto_kolicina', 'jedinica_mjere',
                        'maloprodajna_cijena', 'poseban_oblik_prodaje',
                        'cijena_jedinice_mjere', 'barkod', 'ean_barkod', 'bazna_cijena',
                    ),
                    'services_required_fields' => array(
                        'naziv', 'maloprodajna_cijena', 'poseban_oblik_prodaje', 'bazna_cijena',
                    ),
                    'publication_filename_fields' => array( 'identity', 'outlet_or_all', 'goods_or_services', 'sequence', 'YYYYMMDD_HHMM' ),
                    'classification' => 'manual_verified',
                    'historical_prices' => 'not_inferred',
                    'source_urls' => array(
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1583.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1584.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1578.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1579.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_11_132_1580.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1212.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_101_1213.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1309.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_110_1310.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_06_59_728.html',
                        'https://narodne-novine.nn.hr/clanci/sluzbeni/2026_09_105_1270.html',
                        'https://mingo.gov.hr/vijesti/ministar-susnjar-kroz-dijalog-s-gospodarstvenicima-do-provedivih-pravila-i-vece-zastite-potrosaca/10461',
                    ),
                );
    }

    public function discovery(string $platform = 'web', ?string $publicRoot = null): array
    {
        $base = rtrim($publicRoot ?? $this->base(), '/');
        $published = $this->read('published.json');
        $draft = $this->read('draft.json');
        $endpoint = $base . '/bazna/v1/cjenici';
        $feeds = [];
        $officialFeeds = [];
        foreach (self::KINDS as $kind) {
            $ready = isset($published['published_at']) && $this->officialIdentity() !== '' &&
                $this->officialMissing($kind, '', $published) === [];
            if ($ready) {
                try { $this->officialFilename($kind, 'csv'); }
                catch (\Throwable $error) { $ready = false; }
            }
            $officialFeeds[$kind] = [
                'csv' => $ready ? $base . '/bazna/v1/sluzbeni/' . $kind . '/csv' : '',
                'xml' => $ready ? $base . '/bazna/v1/sluzbeni/' . $kind . '/xml' : '',
                'available_csv' => $ready,
                'available_xml' => $ready,
            ];
            $feeds[$kind] = [
                'csv' => $endpoint . '/' . $kind . '/csv',
                'available_csv' => isset($published['published_at']),
            ];
        }
        $outlets = [];
        foreach ($published['outlets'] ?? [] as $outlet) {
            $o = ['name' => $outlet['name'], 'code' => $outlet['code'], 'address' => $outlet['address'], 'feeds' => []];
            foreach (self::KINDS as $kind) {
                $o['feeds'][$kind] = [
                    'csv' => $feeds[$kind]['csv'] . '?oznaka=' . rawurlencode($outlet['code']),
                    'available_csv' => true,
                ];
            }
            $o['official_feeds'] = [];
            foreach (self::KINDS as $kind) {
                $ready = isset($published['published_at']) && $this->officialIdentity() !== '' &&
                    $this->officialMissing($kind, $outlet['code'], $published) === [];
                if ($ready) {
                    try { $this->officialFilename($kind, 'csv', $outlet['code']); }
                    catch (\Throwable $error) { $ready = false; }
                }
                $o['official_feeds'][$kind] = [
                    'csv' => $ready ? $officialFeeds[$kind]['csv'] ?: $base . '/bazna/v1/sluzbeni/' . $kind . '/csv' : '',
                    'xml' => $ready ? $officialFeeds[$kind]['xml'] ?: $base . '/bazna/v1/sluzbeni/' . $kind . '/xml' : '',
                    'available_csv' => $ready, 'available_xml' => $ready,
                ];
                if ($ready) {
                    $o['official_feeds'][$kind]['csv'] = $base . '/bazna/v1/sluzbeni/' . $kind . '/csv?oznaka=' . rawurlencode($outlet['code']);
                    $o['official_feeds'][$kind]['xml'] = $base . '/bazna/v1/sluzbeni/' . $kind . '/xml?oznaka=' . rawurlencode($outlet['code']);
                }
            }
            $outlets[] = $o;
        }
        return [
            'api_version' => 1, 'plugin_version' => self::VERSION,
            'branding' => ['name' => 'BAZNA', 'description' => 'Bazne cijene'],
            'platform' => $platform,
            'connection' => [
                'mode' => 'public_read_only', 'authentication_required' => false,
                'csv_publication_required' => true, 'rest_endpoint' => $endpoint,
            ],
            'legal' => $this->legal(),
            'published_at' => $published['published_at'] ?? null,
            'cjenik' => $base . '/bazna/cjenik',
            'archive' => $base . '/bazna/arhiva',
            'type_archive' => $base . '/bazna/v1/arhiva',
            'on_request' => $base . '/bazna/v1/usluge-na-upit',
            'feeds' => $feeds, 'official_feeds' => $officialFeeds, 'outlets' => $outlets,
            'publication' => [
                'csv_ready' => isset($published['published_at']),
                'generated_at' => $published['published_at'] ?? null,
                'pending_changes' => $draft !== [] && $draft !== array_intersect_key($published, $draft),
                'compliance_certified' => false,
            ],
        ];
    }

    /**
     * Immutable verified archive snapshot, including the identity used when
     * it was published. Never serve newly edited drafts as historical exports.
     */
    public function archiveSnapshot(string $id): array
    {
        if (!preg_match('/^[0-9]{14}-[a-f0-9]{12}$/D', $id)) {
            throw new InvalidArgumentException('Neispravan identifikator arhive.');
        }
        foreach ($this->read('archive.json')['versions'] ?? [] as $v) {
            if (($v['id'] ?? '') === $id && ($v['file'] ?? '') === 'archive-' . $id . '.json') {
                $snapshot = $this->read('archive-' . $id . '.json');
                if (!isset($snapshot['published_at'])) {
                    throw new InvalidArgumentException('Nepotpuna arhivska snimka.');
                }
                return $snapshot;
            }
        }
        throw new InvalidArgumentException('Arhivski zapis ne postoji.');
    }

    public function officialArchive(string $id, string $kind, string $format, string $outlet = ''): array
    {
        $snapshot = $this->archiveSnapshot($id);
        if ($outlet !== '' &&
            !in_array($outlet, array_column($snapshot['outlets'] ?? [], 'code'), true)) {
            throw new InvalidArgumentException('Poslovnica ne postoji u arhivskom cjeniku.');
        }
        if (!in_array($format, ['csv','xml'], true)) {
            throw new InvalidArgumentException('Nepodržani službeni format.');
        }
        $name = $this->officialFilename($kind, $format, $outlet, $snapshot);
        $body = $format === 'csv'
            ? $this->officialCsv($kind, $outlet, $snapshot)
            : $this->officialXml($kind, $outlet, $snapshot);
        return ['filename' => $name, 'body' => $body, 'format' => $format];
    }

    public function archives(): array
    {
        $out = [];
        $history = $this->read('archive.json')['versions'] ?? [];
        foreach ($history as $v) {
            $id = (string) ($v['id'] ?? '');
            if (!preg_match('/^[0-9]{14}-[a-f0-9]{12}$/D', $id)) continue;
            $date = substr((string) ($v['published_at'] ?? ''), 0, 10);
            // The archived snapshot, not the CURRENT outlet directory, is
            // authoritative. Old outlet links remain stable after renaming or
            // deleting a physical branch in a later publication.
            $snapshot = $this->read('archive-' . $id . '.json');
            $locations = ['webshop'];
            foreach ($snapshot['outlets'] ?? [] as $outlet) {
                $code = $outlet['code'] ?? null;
                if (is_string($code) &&
                    preg_match('/^[A-Za-z0-9][A-Za-z0-9-]{0,63}$/D', $code)) {
                    $locations[] = $code;
                }
            }
            foreach ($locations as $location) {
                foreach (self::KINDS as $kind) {
                    // Keep the original statutory export publicly retrievable
                    // for at least 15 days after replacement (we retain longer).
                    $actualOutlet = $location === 'webshop' ? '' : $location;
                    if (!empty($snapshot['official_identity']) &&
                        $this->officialMissing($kind, $actualOutlet, $snapshot) === []) {
                        foreach (['csv', 'xml'] as $format) {
                            try {
                                $this->officialFilename($kind, $format, $actualOutlet, $snapshot);
                                $out[] = [
                                    'date' => $date, 'kind' => $kind, 'outlet' => $location,
                                    'format' => $format,
                                    'official' => true,
                                    'url' => $this->link('bazna/v1/arhiva/' . $id .
                                        '/sluzbeni/' . $kind . '/' . $format) .
                                        ($location === 'webshop' ? '' : '?oznaka=' . rawurlencode($location)),
                                ];
                            } catch (\Throwable $error) {
                                // Old pre-rulebook snapshots are not falsely marked compliant.
                            }
                        }
                    }
                    $out[] = [
                        'date' => $date, 'kind' => $kind, 'outlet' => $location, 'format' => 'csv',
                        'url' => $this->link('bazna/v1/arhiva/' . $id . '/' . $kind . '/csv') .
                            ($location === 'webshop' ? '' : '?oznaka=' . rawurlencode($location)),
                    ];
                }
            }
        }
        return $out;
    }

    public function archiveCsv(string $id, string $kind, string $outlet = ''): string
    {
        if (!preg_match('/^[0-9]{14}-[a-f0-9]{12}$/D', $id)) {
            throw new InvalidArgumentException('Neispravan identifikator arhive.');
        }
        $history = $this->read('archive.json')['versions'] ?? [];
        foreach ($history as $v) {
            if (($v['id'] ?? '') === $id && ($v['file'] ?? '') === 'archive-' . $id . '.json') {
                $snapshot = $this->read($v['file']);
                if ($outlet !== '') {
                    $known = array_column($snapshot['outlets'] ?? [], 'code');
                    if (!in_array($outlet, $known, true)) {
                        throw new InvalidArgumentException('Poslovnica ne postoji u arhivskoj snimci.');
                    }
                }
                return $this->csv($kind, $outlet, $snapshot);
            }
        }
        throw new InvalidArgumentException('Arhivski zapis ne postoji.');
    }

    public static function esc(string $value): string
    {
        return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
    }
}
