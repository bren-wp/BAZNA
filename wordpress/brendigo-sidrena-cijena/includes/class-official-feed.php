<?php
/**
 * NN 132/2026 (1584) official public CSV/XML projections.
 *
 * The old public feed is intentionally retained for Android/Windows clients.
 * Regulatory exports have their own ordered columns and must not inherit
 * private/admin-only fields from the compatibility export.
 *
 * @package Brendigo_Sidrena_Cijena
 */
defined( 'ABSPATH' ) || exit;

final class Brendigo_Sidrena_Cijena_Official_Feed {
	/** Article 3(2): exact order, with optional columns remaining empty when not applicable. */
	public static function columns( $kind ) {
		if ( 'usluge' === $kind ) {
			return array( 'naziv', 'maloprodajna_cijena', 'poseban_oblik_prodaje', 'bazna_cijena' );
		}
		if ( 'proizvodi' !== $kind ) {
			return array();
		}
		return array(
			'naziv', 'sku', 'marka', 'neto_kolicina', 'jedinica_mjere',
			'maloprodajna_cijena', 'poseban_oblik_prodaje', 'cijena_jedinice_mjere',
			'barkod', 'ean_barkod', 'bazna_cijena',
		);
	}

	/** Map only the legally relevant published values; never infer historical prices. */
	public static function project( $row, $kind ) {
		$names = self::columns( $kind );
		$out = array();
		foreach ( $names as $name ) {
			if ( 'bazna_cijena' === $name ) {
				$value = isset( $row['sidrena_cijena'] ) ? $row['sidrena_cijena'] : '';
			} elseif ( 'poseban_oblik_prodaje' === $name ) {
				$value = ! empty( $row['naziv_posebnog_oblika'] ) ? 'da' : 'ne';
			} elseif ( 'ean_barkod' === $name ) {
				$value = isset( $row['ean_barkod'] ) ? $row['ean_barkod'] : '';
			} else {
				$value = isset( $row[ $name ] ) ? $row[ $name ] : '';
			}
			$out[ $name ] = is_scalar( $value ) ? (string) $value : '';
		}
		return $out;
	}

	/** Returns missing evidence without making an unfounded declaration of compliance. */
	public static function missing_required_data( $rows, $kind ) {
		$missing = array();
		foreach ( $rows as $i => $row ) {
			$projected = self::project( $row, $kind );
			foreach ( array( 'naziv', 'maloprodajna_cijena', 'bazna_cijena' ) as $required ) {
				if ( '' === trim( $projected[ $required ] ) ) {
					$missing[] = array( 'row' => $i + 1, 'field' => $required );
				}
			}
			if ( 'proizvodi' === $kind && '' !== $projected['cijena_jedinice_mjere'] ) {
				foreach ( array( 'neto_kolicina', 'jedinica_mjere' ) as $required ) {
					if ( '' === trim( $projected[ $required ] ) ) {
						$missing[] = array( 'row' => $i + 1, 'field' => $required );
					}
				}
			}
		}
		return $missing;
	}

	public static function csv( $rows, $kind ) {
		$keys = self::columns( $kind );
		if ( ! $keys ) {
			return '';
		}
		$feed = Brendigo_Sidrena_Cijena_Feed_Files::instance();
		$lines = array( $feed->csv_line( $keys ) );
		foreach ( $rows as $row ) {
			$values = self::project( $row, $kind );
			foreach ( array( 'maloprodajna_cijena', 'cijena_jedinice_mjere', 'bazna_cijena' ) as $amount ) {
				if ( isset( $values[ $amount ] ) ) {
					$values[ $amount ] = $feed->csv_amount( $values[ $amount ] );
				}
			}
			$lines[] = $feed->csv_line( array_values( $values ) );
		}
		return "\xEF\xBB\xBF" . implode( '', $lines );
	}

	public static function xml( $rows, $kind ) {
		$keys = self::columns( $kind );
		if ( ! $keys ) {
			return '';
		}
		$item = 'usluge' === $kind ? 'usluga' : 'roba';
		$result = '<?xml version="1.0" encoding="UTF-8"?>' . "\n<cjenik>\n";
		foreach ( $rows as $row ) {
			$values = self::project( $row, $kind );
			$result .= "\t<" . $item . ">\n";
			foreach ( $values as $field => $value ) {
				$result .= "\t\t<" . $field . '>' . esc_xml( $value ) . '</' . $field . ">\n";
			}
			$result .= "\t</" . $item . ">\n";
		}
		return $result . "</cjenik>\n";
	}

	/** Legal filename including identity, outlet/all-outlets, type, sequence and HR-local timestamp. */
	public static function filename( $kind, $ext, $oznaka, $sequence, $timestamp = null ) {
		$settings = Brendigo_Sidrena_Cijena_Settings::instance();
		$identity = (string) $settings->get( 'trader_identifier', '' );
		$type = (string) $settings->get( 'trader_identifier_kind', 'oib' );
		if ( ! self::valid_identifier( $identity, $type ) ||
			! in_array( $kind, array( 'proizvodi', 'usluge' ), true ) ||
			! in_array( $ext, array( 'csv', 'xml' ), true ) ) {
			return '';
		}
		$oznaka = $settings->normalize_oznaka( $oznaka );
		if ( '' === $oznaka && ! $settings->is_multi_poslovnica() ) {
			$place = 'ZA_SVA_PRODAJNA_MJESTA';
		} else {
			$outlet = '' !== $oznaka ? Brendigo_Sidrena_Cijena_Poslovnice::instance()->get_by_oznaka( $oznaka ) : null;
			if ( '' !== $oznaka && ! is_array( $outlet ) ) {
				// Never silently replace an unknown outlet with the webshop.
				return '';
			}
			$oblik  = is_array( $outlet ) && isset( $outlet['oblik'] ) ? $outlet['oblik'] : $settings->get( 'file_oblik', '' );
			$adresa = is_array( $outlet ) && isset( $outlet['adresa'] ) ? $outlet['adresa'] : $settings->get( 'file_adresa', '' );
			$code   = '' === $oznaka ? $settings->normalize_oznaka( $settings->get( 'file_oznaka', '' ) ) : $oznaka;
			// NN 132/2026, 1584 article 6(3): exact outlet form,
			// address and code are part of the official filename.
			// "adresa" / "webshop" fallback placeholders are not evidence.
			if ( ! is_scalar( $oblik ) || ! is_scalar( $adresa ) ||
				'' === trim( (string) $oblik ) ||
				'' === trim( (string) $adresa ) ||
				'' === trim( (string) $code ) ) {
				return '';
			}
			$place = $settings->sanitize_file_part( $oblik, '' )
				. '_' . $settings->sanitize_file_part( $adresa, '' )
				. '_' . $code;
		}
		$ts = null === $timestamp ? time() : (int) $timestamp;
		$date = wp_date( 'Ymd_Hi', $ts, new DateTimeZone( 'Europe/Zagreb' ) );
		return $identity . '_' . $place . '_' . ( 'usluge' === $kind ? 'USLUGE' : 'ROBA' )
			. '_' . max( 1, (int) $sequence ) . '_' . $date . '.' . $ext;
	}

	/** EAN-8 and EAN-13 checksum, preserving leading zeroes. */
	public static function valid_ean( $value ) {
		$value = (string) $value;
		if ( ! preg_match( '/^(?:\\d{8}|\\d{13})$/D', $value ) ) {
			return false;
		}
		$length = strlen( $value ) - 1;
		$sum = 0;
		for ( $i = 0; $i < $length; $i++ ) {
			$sum += (int) $value[$i] * ( ( $length - $i ) % 2 ? 3 : 1 );
		}
		return ( 10 - $sum % 10 ) % 10 === (int) $value[$length];
	}

	/** ISO 7064 MOD 11,10 check digit; all leading zeroes are significant. */
	public static function valid_oib( $value ) {
		$value = (string) $value;
		if ( ! preg_match( '/^\d{11}$/D', $value ) ) {
			return false;
		}
		$state = 10;
		for ( $i = 0; $i < 10; $i++ ) {
			$state = ( $state + (int) $value[ $i ] ) % 10;
			if ( 0 === $state ) {
				$state = 10;
			}
			$state = ( $state * 2 ) % 11;
		}
		$digit = 11 - $state;
		if ( 10 === $digit ) {
			$digit = 0;
		}
		return $digit === (int) $value[10];
	}

	public static function valid_identifier( $value, $type ) {
		$value = (string) $value;
		return ( 'oib' === $type && self::valid_oib( $value ) )
			|| ( 'maticni_broj' === $type && (bool) preg_match( '/^\d{8}$/D', $value ) );
	}
}
