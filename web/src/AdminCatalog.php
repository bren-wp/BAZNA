<?php
/**
 * BAZNA shared accessible catalog editor for PHP and Joomla.
 * One rendering implementation, one UI field mapping, one Core validation
 * path: products, services and outlets no longer require raw JSON editing.
 */
declare(strict_types=1);
namespace Brendigo\Bazna;

use InvalidArgumentException;

final class AdminCatalog
{
    private const TABS = ['proizvodi' => 'products', 'usluge' => 'services', 'poslovnice' => 'outlets', 'na-upit' => 'on_request'];

    public static function collection(string $tab): string
    {
        return self::TABS[$tab] ?? throw new InvalidArgumentException('Nepoznata BAZNA kartica.');
    }

    public static function fromPost(array $post): array
    {
        $row = $post['row'] ?? [];
        if (!is_array($row)) throw new InvalidArgumentException('Neispravan redak.');
        $clean = [];
        $names = array_merge(Core::FIELDS, ['outlet', 'historical_verified', 'code', 'name', 'address', 'kriteriji']);
        foreach ($names as $field) {
            if (!array_key_exists($field, $row)) continue;
            $value = $row[$field];
            if (!is_string($value)) throw new InvalidArgumentException('Neispravan oblik polja: ' . $field);
            if ($field === 'historical_verified') {
                $clean[$field] = $value === '1';
            } else {
                $clean[$field] = trim($value);
            }
        }
        return $clean;
    }

    public static function indexFromPost(array $post): int
    {
        $raw = $post['index'] ?? '-1';
        if (!is_string($raw) || !preg_match('/^-?(?:0|[1-9]\d{0,5})$/D', $raw)) {
            throw new InvalidArgumentException('Neispravan indeks za uređivanje.');
        }
        return (int) $raw;
    }

    private static function input(string $field, string $label, array $values,
        string $type = 'text', bool $required = false, bool $readonly = false): string
    {
        $value = Core::esc((string) ($values[$field] ?? ''));
        $label = Core::esc($label);
        $name = Core::esc($field);
        $inputMode = in_array($field, ['maloprodajna_cijena','sidrena_cijena','cijena_jedinice_mjere'], true)
            ? ' inputmode="decimal"' : '';
        return '<label class="bazna-field">' . $label .
            '<input type="' . Core::esc($type) . '" name="row[' . $name . ']" value="' . $value .
            '" maxlength="800"' . ($required ? ' required' : '') .
            ($readonly ? ' readonly' : '') . $inputMode . '></label>';
    }

    private static function fields(string $collection, array $row, array $outlets, bool $editing): string
    {
        if ($collection === 'outlets') {
            return '<div class="bazna-fields-grid">' .
                self::input('code', 'Oznaka poslovnice', $row, 'text', true, $editing) .
                self::input('name', 'Naziv poslovnice', $row, 'text', true) .
                self::input('address', 'Adresa poslovnice', $row) .
                '</div>';
        }
        if ($collection === 'on_request') {
            $html = '<div class="bazna-fields-grid">' .
                self::input('id', 'Trajni ID (automatski ako je prazan)', $row, 'text', false, $editing) .
                self::input('naziv', 'Naziv usluge na upit', $row, 'text', true) .
                self::input('kriteriji', 'Kriteriji određivanja cijene', $row, 'text', true);
            $selected = (string) ($row['outlet'] ?? '');
            $html .= '<label class="bazna-field">Poslovnica<select name="row[outlet]">';
            $html .= '<option value="">Webshop</option>';
            foreach ($outlets as $outlet) {
                $code = (string) ($outlet['code'] ?? '');
                $html .= '<option value="' . Core::esc($code) . '"' .
                    ($code === $selected ? ' selected' : '') . '>' .
                    Core::esc((string) ($outlet['name'] ?? $code)) . '</option>';
            }
            return $html . '</select></label></div>';
        }
        $html = '<div class="bazna-fields-grid">' .
            self::input('id', 'Trajni ID (automatski ako je prazan)', $row, 'text', false, $editing) .
            self::input('naziv', 'Naziv ' . ($collection === 'products' ? 'proizvoda' : 'usluge'), $row, 'text', true) .
            self::input('maloprodajna_cijena', 'Aktualna cijena (€)', $row, 'text', true) .
            self::input('sidrena_cijena', 'Bazna cijena (€)', $row) .
            self::input('sidrena_datum', 'Datum bazne cijene', $row, 'date') .
            self::input('sku', 'SKU / interna oznaka', $row) .
            self::input('marka', 'Marka / pružatelj', $row) .
            self::input('barkod', 'Barkod', $row) .
            self::input('jedinica_mjere', 'Jedinica mjere', $row) .
            self::input('cijena_jedinice_mjere', 'Cijena po jedinici mjere (€)', $row) .
            self::input('dostupnost', 'Dostupnost', $row) .
            self::input('naziv_posebnog_oblika', 'Naziv posebnog oblika prodaje', $row) .
            self::input('slika_url', 'HTTPS adresa slike', $row, 'url');
        $html .= '<label class="bazna-field">Poslovnica<select name="row[outlet]">';
        $selected = (string) ($row['outlet'] ?? '');
        $html .= '<option value="">Webshop / sve</option>';
        foreach ($outlets as $outlet) {
            $code = (string) ($outlet['code'] ?? '');
            $html .= '<option value="' . Core::esc($code) . '"' . ($code === $selected ? ' selected' : '') .
                '>' . Core::esc((string) ($outlet['name'] ?? $code)) . '</option>';
        }
        $html .= '</select></label></div>';
        $html .= '<label class="bazna-check"><input type="hidden" name="row[historical_verified]" value="0">' .
            '<input type="checkbox" name="row[historical_verified]" value="1"' .
            (!empty($row['historical_verified']) ? ' checked' : '') .
            '> Potvrđujem da za baznu cijenu postoji izvorni dokaz o iznosu na navedeni datum.</label>';
        return $html;
    }

    public static function render(string $tab, array $draft, string $url,
        string $csrfHtml, bool $active, int $page = 1): string
    {
        $collection = self::collection($tab);
        $rows = $draft[$collection] ?? [];
        if (!is_array($rows)) $rows = [];
        $page = max(1, min(1000, $page));
        $start = ($page - 1) * 60;
        $outlets = $draft['outlets'] ?? [];
        $disabled = $active ? '' : ' disabled';
        $headline = ['products' => 'Proizvodi', 'services' => 'Usluge', 'outlets' => 'Poslovnice', 'on_request' => 'Cijena na upit'][$collection];
        $html = '<section class="bazna-panel bazna-catalog-editor"><h2>' . Core::esc($headline) .
            '</h2><p class="bazna-muted">Svi podaci ostaju u nacrtu do zasebne objave. ' .
            'Bazna cijena unosi se samo na temelju dokaza, a pravilo najniže cijene u 30 dana je neovisno.</p>';

        $html .= '<details class="bazna-entry bazna-new-entry"' . (!$rows ? ' open' : '') .
            '><summary><strong>+ Dodaj ' . ($collection === 'outlets' ? 'poslovnicu' :
            ($collection === 'products' ? 'proizvod' : 'uslugu')) . '</strong></summary>';
        $html .= '<form method="post" action="' . Core::esc($url) . '">' . $csrfHtml .
            '<input type="hidden" name="action" value="save_item">' .
            '<input type="hidden" name="collection" value="' . Core::esc($collection) . '">' .
            '<input type="hidden" name="index" value="-1">' .
            self::fields($collection, [], $outlets, false) .
            '<button type="submit"' . $disabled . '>Spremi novu stavku</button></form></details>';

        $html .= '<div class="bazna-entry-count" role="status">Ukupno: <strong>' .
            count($rows) . '</strong> · Prikaz: ' . (count($rows) ? ($start + 1) : 0) .
            '–' . min($start + 60, count($rows)) . '</div>';
        foreach (array_slice($rows, $start, 60, true) as $index => $row) {
            if (!is_array($row)) continue;
            $title = (string) ($row[$collection === 'outlets' ? 'name' : 'naziv'] ?? '');
            $detail = $collection === 'outlets'
                ? (string) ($row['code'] ?? '')
                : ($collection === 'on_request' ? 'Cijena na upit' : (string) ($row['maloprodajna_cijena'] ?? '') . ' €');
            $hash = hash('sha256', json_encode($row,
                JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR));
            $identity = '<input type="hidden" name="collection" value="' . Core::esc($collection) . '">' .
                '<input type="hidden" name="index" value="' . $index . '">' .
                '<input type="hidden" name="expected_hash" value="' . $hash . '">';
            $html .= '<details class="bazna-entry"><summary><span class="bazna-entry-name">' .
                Core::esc($title) . '</span><span class="bazna-entry-price">' .
                Core::esc($detail) . '</span></summary>';
            $html .= '<form method="post" action="' . Core::esc($url) . '">' . $csrfHtml .
                $identity . '<input type="hidden" name="action" value="save_item">' .
                self::fields($collection, $row, $outlets, true) .
                '<button type="submit"' . $disabled . '>Spremi izmjene</button></form>';
            $html .= '<form class="bazna-remove-form" method="post" action="' . Core::esc($url) . '">' .
                $csrfHtml . $identity .
                '<input type="hidden" name="action" value="delete_item">' .
                '<label class="bazna-check"><input type="checkbox" name="delete_confirm" value="1" required>' .
                ' Potvrđujem uklanjanje ove stavke iz nacrta.</label>' .
                '<button class="bazna-danger" type="submit"' . $disabled . '>Ukloni iz nacrta</button></form></details>';
        }
        $totalPages = (int) ceil(count($rows) / 60);
        if ($totalPages > 1) {
            $html .= '<nav class="bazna-pagination" aria-label="Stranice kataloga">';
            for ($i = max(1, $page - 2); $i <= min($totalPages, $page + 2); ++$i) {
                $html .= '<a href="' . Core::esc($url . '&page=' . $i) . '"' .
                    ($i === $page ? ' aria-current="page"' : '') . '>Stranica ' . $i . '</a>';
            }
            $html .= '</nav>';
        }
        return $html . '</section>';
    }
}
