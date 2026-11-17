<?php
/**
 * Authenticated, optimistic standalone-draft sync. Never publishes prices.
 * @package Brendigo_Sidrena_Cijena
 */
defined( 'ABSPATH' ) || exit;

final class Brendigo_Sidrena_Cijena_Sync_Draft {
    const LOCK_KEY = 'bazna_sync_draft_lock_v1';
    const MAX_BODY = 262144;
    const MAX_ROWS = 5000;

    public static function register() {
        register_rest_route( 'brendigo-bazna/v1', '/sync/draft', array(
            array(
                'methods' => WP_REST_Server::READABLE,
                'callback' => array( __CLASS__, 'read' ),
                'permission_callback' => array( __CLASS__, 'authorize' ),
            ),
            array(
                'methods' => WP_REST_Server::CREATABLE,
                'callback' => array( __CLASS__, 'write' ),
                'permission_callback' => array( __CLASS__, 'authorize' ),
            ),
        ) );
    }

    /** WP Application Passwords or nonce-authenticated sessions; always HTTPS. */
    public static function authorize( $request = null ) {
        if ( ! is_ssl() ) {
            return new WP_Error( 'bazna_sync_https', 'BAZNA sinkronizacija zahtijeva HTTPS.', array( 'status' => 403 ) );
        }
        if ( ! Brendigo_Sidrena_Cijena_Plugin::current_user_can_manage() ) {
            return new WP_Error( 'bazna_sync_forbidden', 'Nedovoljne ovlasti za sinkronizaciju.', array( 'status' => 403 ) );
        }
        if ( Brendigo_Sidrena_Cijena_Plugin::woocommerce_active() ) {
            // WooCommerce needs per-product conflict control; never overwrite
            // its source-of-truth catalog using the standalone option.
            return new WP_Error( 'bazna_sync_woo_unsupported', 'Ova sinkronizacija vrijedi samo za samostalni BAZNA katalog.', array( 'status' => 409 ) );
        }
        return true;
    }

    public static function revision( $rows ) {
        return hash( 'sha256', wp_json_encode( array_values( $rows ), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES ) );
    }

    private static function snapshot() {
        $rows = Brendigo_Sidrena_Cijena_Catalog::instance()->all();
        return array( 'schema' => 1, 'mode' => 'standalone_draft', 'revision' => self::revision( $rows ), 'rows' => $rows );
    }

    private static function no_store( $data ) {
        $response = rest_ensure_response( $data );
        $response->header( 'Cache-Control', 'private, no-store, max-age=0' );
        $response->header( 'X-Content-Type-Options', 'nosniff' );
        return $response;
    }

    public static function read( $request = null ) {
        $permission = self::authorize( $request );
        return true === $permission ? self::no_store( self::snapshot() ) : $permission;
    }

    /** add_option is atomic by unique name. Conditional SQL prevents removing another owner's lock. */
    private static function acquire_lock() {
        $raw = wp_json_encode( array( 'token' => bin2hex( random_bytes( 16 ) ), 'until' => time() + 120 ) );
        if ( add_option( self::LOCK_KEY, $raw, '', false ) ) {
            return $raw;
        }
        $previous = get_option( self::LOCK_KEY, '' );
        $data = is_string( $previous ) ? json_decode( $previous, true ) : null;
        if ( is_array( $data ) && isset( $data['until'] ) && (int) $data['until'] < time() ) {
            self::release_lock( $previous );
            if ( add_option( self::LOCK_KEY, $raw, '', false ) ) {
                return $raw;
            }
        }
        return '';
    }

    private static function release_lock( $raw ) {
        global $wpdb;
        if ( ! is_string( $raw ) || '' === $raw ) {
            return;
        }
        $wpdb->query( $wpdb->prepare(
            "DELETE FROM {$wpdb->options} WHERE option_name = %s AND option_value = %s",
            self::LOCK_KEY, $raw
        ) );
        wp_cache_delete( self::LOCK_KEY, 'options' );
        wp_cache_delete( 'notoptions', 'options' );
    }

    /** Exactly one row per write: no bulk replacement, no public publication. */
    public static function write( $request ) {
        $permission = self::authorize( $request );
        if ( true !== $permission ) {
            return $permission;
        }
        if ( (int) $request->get_header( 'content-length' ) > self::MAX_BODY ) {
            return new WP_Error( 'bazna_sync_size', 'Prevelik zahtjev.', array( 'status' => 413 ) );
        }
        $raw = $request->get_body();
        if ( ! is_string( $raw ) || strlen( $raw ) > self::MAX_BODY ) {
            return new WP_Error( 'bazna_sync_body', 'Prevelik ili neispravan zahtjev.', array( 'status' => 413 ) );
        }
        $input = json_decode( $raw, true );
        if ( ! is_array( $input ) || ! isset( $input['expected_revision'], $input['operation'] ) ||
            ! is_string( $input['expected_revision'] ) || ! preg_match( '/^[a-f0-9]{64}$/D', $input['expected_revision'] ) ||
            ! in_array( $input['operation'], array( 'upsert', 'delete' ), true ) ) {
            return new WP_Error( 'bazna_sync_format', 'Neispravan ugovor sinkronizacije.', array( 'status' => 400 ) );
        }
        $lock = self::acquire_lock();
        if ( '' === $lock ) {
            return new WP_Error( 'bazna_sync_locked', 'Sinkronizacija je zauzeta.', array( 'status' => 423 ) );
        }
        try {
            wp_cache_delete( Brendigo_Sidrena_Cijena_Catalog::OPTION_KEY, 'options' );
            wp_cache_delete( 'alloptions', 'options' );
            Brendigo_Sidrena_Cijena_Catalog::instance()->refresh_for_sync();
            $state = self::snapshot();
            if ( ! hash_equals( $state['revision'], $input['expected_revision'] ) ) {
                return new WP_Error( 'bazna_sync_conflict', 'Nacrt je izmijenjen; preuzmite novu verziju.',
                    array( 'status' => 409, 'revision' => $state['revision'] ) );
            }
            $rows = $state['rows'];
            if ( count( $rows ) > self::MAX_ROWS ) {
                return new WP_Error( 'bazna_sync_catalog_size', 'Prevelik katalog.', array( 'status' => 413 ) );
            }
            $id = isset( $input['id'] ) && is_string( $input['id'] ) ? $input['id'] : '';
            if ( '' !== $id && ! preg_match( '/^[1-9][0-9]{0,11}$/D', $id ) ) {
                return new WP_Error( 'bazna_sync_id', 'Neispravna oznaka retka.', array( 'status' => 400 ) );
            }
            $index = null;
            foreach ( $rows as $i => $entry ) {
                if ( '' !== $id && (string) $entry['id'] === $id ) {
                    $index = $i;
                    break;
                }
            }
            if ( '' !== $id && null === $index ) {
                return new WP_Error( 'bazna_sync_missing', 'Stavka više ne postoji.', array( 'status' => 409 ) );
            }
            if ( 'delete' === $input['operation'] ) {
                if ( null === $index || true !== ( $input['confirm_delete'] ?? false ) ) {
                    return new WP_Error( 'bazna_sync_delete', 'Brisanje zahtijeva postojeći ID i potvrdu.', array( 'status' => 400 ) );
                }
                array_splice( $rows, $index, 1 );
            } else {
                $row = $input['row'] ?? null;
                if ( ! is_array( $row ) || count( $row ) > 40 || ! isset( $row['naziv'], $row['cijena_redovna'] ) ||
                    ! is_string( $row['naziv'] ) || '' === trim( $row['naziv'] ) || ! is_scalar( $row['cijena_redovna'] ) ) {
                    return new WP_Error( 'bazna_sync_row', 'Nedostaju obvezni podaci retka.', array( 'status' => 400 ) );
                }
                $fields = array( 'id', 'naziv', 'sku', 'barkod', 'neto_kolicina', 'ean_barkod',
                    'marka', 'url', 'cijena_redovna', 'cijena_akcijska', 'sidrena_cijena', 'dostupnost',
                    'cjm', 'cjm_suffix', 'naziv_posebnog_oblika', 'jedinica_mjere',
                    'cijena_jedinice_mjere', 'vrsta', 'sidrena_datum', 'historical_source',
                    'reference_scope', 'historical_method', 'poslovnica' );
                foreach ( $row as $field => $value ) {
                    if ( ! in_array( $field, $fields, true ) || ! is_scalar( $value ) || strlen( (string) $value ) > 2048 ) {
                        return new WP_Error( 'bazna_sync_fields', 'Nedopušteno ili predugačko polje.', array( 'status' => 400 ) );
                    }
                }
                if ( isset( $row['id'] ) && (string) $row['id'] !== $id ) {
                    return new WP_Error( 'bazna_sync_row_id', 'ID postojeće stavke ne može se mijenjati.', array( 'status' => 400 ) );
                }
                $row = null === $index ? $row : array_merge( $rows[ $index ], $row );
                $row['id'] = $id;
                // Never accept remotely supplied documentary attestation.
                // Catalog::save also revokes old attestation on changed historic fields.
                $row['historical_verified'] = null === $index ? '' : ( $rows[ $index ]['historical_verified'] ?? '' );
                if ( null === $index ) {
                    if ( count( $rows ) >= self::MAX_ROWS ) {
                        return new WP_Error( 'bazna_sync_limit', 'Katalog je popunjen.', array( 'status' => 413 ) );
                    }
                    $rows[] = $row;
                } else {
                    $rows[ $index ] = $row;
                }
            }
            $after = Brendigo_Sidrena_Cijena_Catalog::instance()->save( $rows );
            if ( self::revision( $after ) === $state['revision'] && $rows !== $state['rows'] ) {
                return new WP_Error( 'bazna_sync_storage', 'Nije potvrđena promjena u pohrani.', array( 'status' => 500 ) );
            }
            return self::no_store( array( 'ok' => true, 'schema' => 1,
                'revision' => self::revision( $after ), 'published' => false ) );
        } finally {
            self::release_lock( $lock );
        }
    }
}
