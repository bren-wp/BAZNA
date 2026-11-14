<?php
/** Read-only REST catalog discovery and feed endpoints. */
defined( 'ABSPATH' ) || exit;

final class Brendigo_Sidrena_Cijena_Public_API {
	private static $instance;

	public static function instance() {
		if ( null === self::$instance ) {
			self::$instance = new self();
		}
		return self::$instance;
	}
	private function __construct() {
		add_action( 'rest_api_init', array( $this, 'register' ) );
	}

	public function register() {
		// New BAZNA-facing paths are canonical; legacy paths stay read-only
		// and fully functional for installed Android clients and old embeds.
		foreach ( array( 'brendigo-bazna/v1', 'brendigo-sidrena/v1' ) as $namespace ) {
		register_rest_route( $namespace, '/cjenici', array(
			'methods'             => WP_REST_Server::READABLE,
			'callback'            => array( $this, 'index' ),
			'permission_callback' => '__return_true',
		) );
		register_rest_route( $namespace, '/arhiva', array(
			'methods' => WP_REST_Server::READABLE,
			'callback' => array( $this, 'archive_index' ),
			'permission_callback' => '__return_true',
			'args' => array(
				'kind' => array( 'type' => 'string', 'enum' => array( 'proizvodi', 'usluge' ) ),
				'outlet' => array( 'type' => 'string', 'sanitize_callback' => 'sanitize_text_field' ),
			),
		) );
		register_rest_route( $namespace, '/cjenici/(?P<kind>proizvodi|usluge)/(?P<format>xml|csv)', array(
			'methods'             => WP_REST_Server::READABLE,
			'callback'            => array( $this, 'feed' ),
			'permission_callback' => '__return_true',
			'args'                => array( 'oznaka' => array( 'type' => 'string', 'default' => '', 'sanitize_callback' => 'sanitize_text_field' ) ),
		) );
		}
	}

	/**
	 * Read-only feed readiness flag used by mobile and public catalog clients.
	 * No private metadata or filesystem path is exposed to visitors.
	 *
	 * @param string $kind Product or service feed.
	 * @param string $format CSV or XML.
	 * @param string $oznaka Optional public outlet code.
	 * @return bool
	 */
	private function feed_available( $kind, $format = 'csv', $oznaka = '' ) {
		$path = Brendigo_Sidrena_Cijena_Feed_Files::instance()->get_type_public_path( $kind, $format, $oznaka );
		return is_string( $path ) && '' !== $path && is_file( $path ) && is_readable( $path );
	}

	public function index() {
		$export = Brendigo_Sidrena_Cijena_Export::instance();
		$meta = $export->get_public_meta();
		$out = array(
			'api_version'  => 1,
			'platform'     => 'wordpress',
			'plugin_version' => BRENDIGO_SIDRENA_CIJENA_VERSION,
            'branding' => array(
                'name' => 'BAZNA',
                'description' => 'Bazne cijene',
            ),
            // No license key, admin identity or secret ever leaves the server.
            'licensing' => array(
                'required' => true,
                'active' => Brendigo_Sidrena_Cijena_License::valid(),
            ),
			// Public-only Android integration. No credentials, session data, or
			// unpublished WooCommerce metadata are available through this route.
			'connection' => array(
				'mode' => 'public_read_only',
				'authentication_required' => false,
				'csv_publication_required' => true,
				'rest_endpoint' => esc_url_raw( rest_url( 'brendigo-bazna/v1/cjenici' ) ),
			),
			'legal' => Brendigo_Sidrena_Cijena_Legal::public_contract(),
			'published_at' => isset( $meta['generated'] ) ? gmdate( 'c', (int) $meta['generated'] ) : null,
			'cjenik'       => esc_url_raw( $export->get_cjenik_url() ),
			'archive'      => esc_url_raw( $export->get_archive_url() ),
			'type_archive' => esc_url_raw( rest_url( 'brendigo-bazna/v1/arhiva' ) ),
			'combined'     => esc_url_raw( $export->get_public_url() ),
			'feeds'        => array(),
			'outlets'      => array(),
		);
		foreach ( array( 'proizvodi', 'usluge' ) as $kind ) {
			$out['feeds'][ $kind ] = array( 'available_csv' => $this->feed_available( $kind ) );
			foreach ( array( 'xml', 'csv' ) as $ext ) {
				$out['feeds'][ $kind ][ $ext ] = esc_url_raw( rest_url( 'brendigo-bazna/v1/cjenici/' . $kind . '/' . $ext ) );
			}
		}
		if ( Brendigo_Sidrena_Cijena_Settings::instance()->is_multi_poslovnica() ) {
			foreach ( Brendigo_Sidrena_Cijena_Poslovnice::instance()->all() as $outlet ) {
				$code = isset( $outlet['oznaka'] ) ? (string) $outlet['oznaka'] : '';
				if ( '' === $code ) {
					continue;
				}
				$entry = array(
                    'name' => isset( $outlet['naziv'] ) ? $outlet['naziv'] : $code,
                    'code' => $code,
                    // Outlet addresses already appear in public price-list
                    // filenames. Publish only the sanitized business address;
                    // never send internal IDs, assignments or stock records.
                    'address' => isset( $outlet['adresa'] ) ? sanitize_text_field( (string) $outlet['adresa'] ) : '',
                    'feeds' => array(),
                );
				foreach ( array( 'proizvodi', 'usluge' ) as $kind ) {
					$entry['feeds'][ $kind ] = array( 'available_csv' => $this->feed_available( $kind, 'csv', $code ) );
				}
				foreach ( array( 'proizvodi', 'usluge' ) as $kind ) {
					foreach ( array( 'xml', 'csv' ) as $ext ) {
						$entry['feeds'][ $kind ][ $ext ] = esc_url_raw( add_query_arg( 'oznaka', $code, rest_url( 'brendigo-bazna/v1/cjenici/' . $kind . '/' . $ext ) ) );
					}
				}
				$out['outlets'][] = $entry;
			}
		}
		// A published CSV may be older than WooCommerce edits still awaiting
		// the next public rebuild. Expose this distinction without private data.
		$pending = array();
		if ( class_exists( 'Brendigo_Sidrena_Cijena_Automation' ) ) {
			$pending = Brendigo_Sidrena_Cijena_Automation::instance()->get_pending_state();
		}
		$pending_changes = is_array( $pending ) && ! empty( $pending['since'] );
		// Each outlet has independent published CSV paths. A valid webshop feed
		// does not prove that every outlet's files were generated.
		$all_outlets_ready = true;
		foreach ( $out['outlets'] as $outlet ) {
			if ( ! $outlet['feeds']['proizvodi']['available_csv'] ||
				! $outlet['feeds']['usluge']['available_csv'] ) {
				$all_outlets_ready = false;
				break;
			}
		}
		// Optional, backwards-compatible status: old clients still use feeds[*].csv.
		$out['publication'] = array(
			'csv_ready' => $out['feeds']['proizvodi']['available_csv'] && $out['feeds']['usluge']['available_csv'],
			'csv_ready_all_outlets' => $out['feeds']['proizvodi']['available_csv'] && $out['feeds']['usluge']['available_csv'] && $all_outlets_ready,
			'generated_at' => $out['published_at'],
			'pending_changes' => $pending_changes,
			// This technical status is never certification of legal price accuracy.
			'compliance_certified' => false,
		);
		// Discovery includes live publication/readiness flags. Never serve a
		// cached "not published" or "no pending changes" status to Android.
		$response = rest_ensure_response( $out );
		$response->header( 'Cache-Control', 'no-store, max-age=0, must-revalidate' );
		return $response;
	}

	public function archive_index( $request ) {
		$kind = $request->get_param( 'kind' );
		$outlet = $request->get_param( 'outlet' );
		$response = rest_ensure_response( Brendigo_Sidrena_Cijena_Archive::instance()->separate_archive_files( $kind ? $kind : '', $outlet ? sanitize_key( $outlet ) : '' ) );
		$response->header( 'Cache-Control', 'no-store, max-age=0, must-revalidate' );
		return $response;
	}

	public function feed( $request ) {
		$kind = $request->get_param( 'kind' );
		$ext = $request->get_param( 'format' );
		$oznaka = Brendigo_Sidrena_Cijena_Settings::instance()->normalize_oznaka( $request->get_param( 'oznaka' ) );
		if ( '' !== $oznaka && ! Brendigo_Sidrena_Cijena_Poslovnice::instance()->get_by_oznaka( $oznaka ) ) {
			return new WP_Error( 'sidrena_unknown_outlet', __( 'Nepoznata poslovnica.', 'brendigo-sidrena-cijena' ), array( 'status' => 404 ) );
		}
		$files = Brendigo_Sidrena_Cijena_Feed_Files::instance();
		$path = $files->get_type_public_path( $kind, $ext, $oznaka );
		if ( ! $path || ! is_file( $path ) || ! is_readable( $path ) ) {
			return new WP_Error( 'sidrena_feed_missing', __( 'Cjenik još nije objavljen.', 'brendigo-sidrena-cijena' ), array( 'status' => 404 ) );
		}
		// Read only already-published files. Never regenerate on a public REST hit.
		$response = new WP_REST_Response( null, 302 );
		$response->header( 'Location', $files->get_type_public_url( $kind, $ext, $oznaka ) );
		// A cached 302 can point at an older published file after rebuilding.
		$response->header( 'Cache-Control', 'no-store, max-age=0, must-revalidate' );
		return $response;
	}
}
