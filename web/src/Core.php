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
    public const VERSION = '1.0.0';
    public const FIELDS = [
        'id', 'naziv', 'vrsta', 'sku', 'marka', 'jedinica_mjere',
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

    public static function validateCatalog(array $document): array
    {
        $outlets = $document['outlets'] ?? [];
        $products = $document['products'] ?? [];
        $services = $document['services'] ?? [];
        if (!is_array($outlets) || !array_is_list($outlets) || count($outlets) > 500 ||
            !is_array($products) || !array_is_list($products) || count($products) > 10000 ||
            !is_array($services) || !array_is_list($services) || count($services) > 10000) {
            throw new InvalidArgumentException('Neispravna veličina kataloga ili poslovnica.');
        }
        $seen = [];
        $cleanOutlets = [];
        foreach ($outlets as $outlet) {
            if (!is_array($outlet)) throw new InvalidArgumentException('Neispravna poslovnica.');
            $code = trim((string) ($outlet['code'] ?? ''));
            $name = trim((string) ($outlet['name'] ?? ''));
            $address = trim((string) ($outlet['address'] ?? ''));
            if (!preg_match('/^[A-Za-z0-9][A-Za-z0-9-]{0,63}$/D', $code) ||
                isset($seen[strtolower($code)]) || $name === '' || strlen($name) > 240 ||
                strlen($address) > 400 || preg_match('/[\x00-\x1F]/', $address)) {
                throw new InvalidArgumentException('Oznaka, naziv ili adresa poslovnice nisu valjani.');
            }
            $seen[strtolower($code)] = true;
            $cleanOutlets[] = ['code' => $code, 'name' => $name, 'address' => $address];
        }
        $result = ['outlets' => $cleanOutlets, 'products' => [], 'services' => []];
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
        if (!is_file($path)) return ['outlets' => [], 'products' => [], 'services' => []];
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
        if (!in_array($collection, ['products', 'services', 'outlets'], true) ||
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

    public function publish(): array
    {
        return $this->catalogTransaction(function (): array {
            $catalog = self::validateCatalog($this->read('draft.json'));
            $existing = $this->read('published.json');
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
            $this->write('published.json', $catalog);
            return $catalog;
        });
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

    public function legal(): array
    {
        return [
            'schema' => 1, 'jurisdiction' => 'HR',
            'rules_revision' => 'NN-101-2026-NN-110-2026',
            'effective_from' => '2026-11-17',
            'reference_dates' => self::REFERENCE_DATES,
            'archive_min_days' => 30,
            'product_publication_deadline' => '08:00',
            'base_price_regulation_status' => 'draft_consultation',
            'historical_price_verification_required' => true,
            'legal_compliance_certified' => false,
            'discount_baseline_is_anchor_price' => false,
            'consumer_30_day_low_is_base_price' => false,
            'implementation' => [
                'published_decisions_ready_from' => '2026-11-17',
                'reference_price_data_entry' => 'verified_manual',
                'product_service_publication' => 'csv_xml_html',
                'publication_source_of_truth' => 'platform_published_feeds',
                'unpublished_draft_features' => 'not_automatically_enabled',
                'discount_30_day_low_independent' => true,
                'automatic_historical_inference' => false,
            ],
        ];
    }

    public function discovery(string $platform = 'web', ?string $publicRoot = null): array
    {
        $base = rtrim($publicRoot ?? $this->base(), '/');
        $published = $this->read('published.json');
        $draft = $this->read('draft.json');
        $endpoint = $base . '/bazna/v1/cjenici';
        $feeds = [];
        foreach (self::KINDS as $kind) {
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
            'feeds' => $feeds, 'outlets' => $outlets,
            'publication' => [
                'csv_ready' => isset($published['published_at']),
                'generated_at' => $published['published_at'] ?? null,
                'pending_changes' => $draft !== [] && $draft !== array_intersect_key($published, $draft),
                'compliance_certified' => false,
            ],
        ];
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
