<?php
/**
 * Explicit merchant-approved one-way Shopify GraphQL sync. No historical base
 * price is fabricated from compareAtPrice; manually verified history is retained.
 */
declare(strict_types=1);
use Brendigo\Bazna\Core;

function bazna_shopify_sync(Core $core, string $shop, string $token): int
{
    if (strlen($token) < 20 || !function_exists('curl_init')) {
        throw new RuntimeException('Nedostaje ovlašteni Shopify Admin API token.');
    }
    $api = 'https://' . $shop . '/admin/api/2026-10/graphql.json';
    $query = <<<'GQL'
query BaznaVariants($cursor: String) {
  shop { currencyCode }
  productVariants(first: 100, after: $cursor) {
    pageInfo { hasNextPage endCursor }
    edges { node {
      id displayName price sku barcode availableForSale
      product { title vendor onlineStoreUrl featuredImage { url } }
    } }
  }
}
GQL;
    $draft = $core->read('draft.json');
    $existing = [];
    foreach ($draft['products'] ?? [] as $item) $existing[$item['id']] = $item;
    $products = [];
    $cursor = null;
    for ($page = 0; $page < 100; ++$page) {
        $body = json_encode(['query' => $query, 'variables' => ['cursor' => $cursor]], JSON_THROW_ON_ERROR);
        $ch = curl_init($api);
        if ($ch === false) throw new RuntimeException('Shopify HTTPS kanal nije dostupan.');
        curl_setopt_array($ch, [
            CURLOPT_POST => true, CURLOPT_POSTFIELDS => $body, CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json', 'X-Shopify-Access-Token: ' . $token,
            ], CURLOPT_FOLLOWLOCATION => false, CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_SSL_VERIFYPEER => true, CURLOPT_CONNECTTIMEOUT => 5,
            CURLOPT_TIMEOUT => 20, CURLOPT_MAXREDIRS => 0,
        ]);
        try {
            $response = curl_exec($ch);
            $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
        } finally { curl_close($ch); }
        if ($status !== 200 || !is_string($response) || strlen($response) > 6 * 1024 * 1024) {
            throw new RuntimeException('Shopify API nije vratio valjan ograničen odgovor.');
        }
        $json = json_decode($response, true, 24, JSON_THROW_ON_ERROR);
        if (!empty($json['errors']) || !isset($json['data']['productVariants'])) {
            throw new RuntimeException('Shopify GraphQL greška ili nedostatne dozvole.');
        }
        if (($json['data']['shop']['currencyCode'] ?? '') !== 'EUR') {
            throw new RuntimeException('Shopify trgovina mora koristiti EUR prije javne objave u ovom adapteru.');
        }
        foreach ($json['data']['productVariants']['edges'] as $edge) {
            $node = $edge['node'] ?? [];
            $id = (string) ($node['id'] ?? '');
            if (!str_starts_with($id, 'gid://shopify/ProductVariant/')) continue;
            $old = $existing[$id] ?? [];
            $product = $node['product'] ?? [];
            $products[] = [
                'id' => $id, 'naziv' => (string) ($node['displayName'] ?? $product['title'] ?? ''),
                'vrsta' => 'proizvod', 'sku' => (string) ($node['sku'] ?? ''),
                'marka' => (string) ($product['vendor'] ?? ''),
                'jedinica_mjere' => '', 'cijena_jedinice_mjere' => '',
                'maloprodajna_cijena' => (string) ($node['price'] ?? ''),
                'naziv_posebnog_oblika' => '',
                // Shopify compareAtPrice is NOT evidence of historical base
                // price or minimum pre-discount price. Only preserve merchant
                // verified history for exactly the same stable variant ID.
                'sidrena_cijena' => ($old['historical_verified'] ?? false) ? ($old['sidrena_cijena'] ?? '') : '',
                'sidrena_datum' => ($old['historical_verified'] ?? false) ? ($old['sidrena_datum'] ?? '') : '',
                'historical_verified' => ($old['historical_verified'] ?? false) === true,
                'barkod' => (string) ($node['barcode'] ?? ''),
                'dostupnost' => !empty($node['availableForSale']) ? 'Dostupno' : 'Nedostupno',
                'slika_url' => (string) ($product['featuredImage']['url'] ?? ''),
                'outlet' => '',
            ];
        }
        if (count($products) > 10000) throw new RuntimeException('Prevelik Shopify katalog.');
        $pageInfo = $json['data']['productVariants']['pageInfo'];
        if (empty($pageInfo['hasNextPage'])) {
            $draft['products'] = $products;
            $core->saveDraft($draft);
            return count($products);
        }
        $next = $pageInfo['endCursor'] ?? null;
        if (!is_string($next) || $next === $cursor || strlen($next) > 1024) {
            throw new RuntimeException('Shopify paginacija je neispravna.');
        }
        $cursor = $next;
    }
    throw new RuntimeException('Shopify katalog prelazi dopušten broj stranica API-ja.');
}
