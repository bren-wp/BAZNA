<?php
/**
 * Settings tab markup.
 *
 * @package Brendigo_Sidrena_Cijena
 */

defined( 'ABSPATH' ) || exit;

$sidrena_woo = Brendigo_Sidrena_Cijena_Plugin::woocommerce_active();
?>
<section class="brendigo-settings-legal">
	<span class="brendigo-overview-icon brendigo-overview-icon--shield"><span class="dashicons dashicons-shield" aria-hidden="true"></span></span>
	<div>
		<p class="brendigo-section-kicker"><?php echo esc_html__( 'Pravna napomena', 'brendigo-sidrena-cijena' ); ?></p>
		<h2><?php echo esc_html__( 'Prikaz bazne cijene', 'brendigo-sidrena-cijena' ); ?></h2>
		<p><?php echo esc_html__( 'Odredite naziv, datum i način prikaza referentne cijene uz proizvod.', 'brendigo-sidrena-cijena' ); ?></p>
	</div>
</section>

<form method="post" action="<?php echo esc_url( $this->get_settings_tab_url( 'postavke' ) ); ?>" class="brendigo-settings-form">
	<?php wp_nonce_field( 'sidrena_cijena_save_settings', 'sidrena_cijena_nonce' ); ?>
	<input type="hidden" name="sidrena_cijena_action" value="save_settings" />
	<input type="hidden" id="sidrena_cijena_default_date" value="<?php echo esc_attr( Brendigo_Sidrena_Cijena_Settings::REFERENCE_DATE_GENERAL ); ?>" />

	<div class="brendigo-settings-grid">
		<section class="sidrena-cijena-card brendigo-settings-card">
			<div class="sidrena-cijena-card__head brendigo-card-title">
				<span class="dashicons dashicons-desktop" aria-hidden="true"></span>
				<div><h2><?php echo esc_html( $sidrena_woo ? __( 'Prikaz na trgovini', 'brendigo-sidrena-cijena' ) : __( 'Prikaz na web-stranici', 'brendigo-sidrena-cijena' ) ); ?></h2><p><?php echo esc_html( $sidrena_woo ? __( 'Kontrolirajte način na koji se bazna cijena prikazuje kupcu.', 'brendigo-sidrena-cijena' ) : __( 'Postavite prikaz bazne cijene za samostalne proizvode i usluge.', 'brendigo-sidrena-cijena' ) ); ?></p></div>
			</div>

			<?php if ( $sidrena_woo ) : ?>
			<div class="brendigo-setting-row">
				<div><strong><?php echo esc_html__( 'Automatski prikaz bazne cijene', 'brendigo-sidrena-cijena' ); ?></strong><small><?php echo esc_html__( 'Prikaži baznu cijenu uz WooCommerce cijenu proizvoda.', 'brendigo-sidrena-cijena' ); ?></small></div>
				<?php $this->render_toggle( 'sidrena_cijena_auto_display', 'sidrena_cijena_auto_display', 'yes' === $sidrena_settings->get( 'auto_display' ), __( 'Automatski prikaz', 'brendigo-sidrena-cijena' ) ); ?>
			</div>
			<div class="brendigo-setting-row">
				<div><strong><?php echo esc_html__( 'Povijesna cijena unosi se uz dokaz', 'brendigo-sidrena-cijena' ); ?></strong>
				<small><?php echo esc_html__( 'Automatsko preslikavanje današnje redovne cijene u baznu cijenu isključeno je. Upišite stvarnu povijesnu cijenu i odgovarajući datum za svaku stavku; sačuvajte izvorne dokaze.', 'brendigo-sidrena-cijena' ); ?></small></div>
				<span class="sidrena-android-bridge__status is-pending"><?php echo esc_html__( 'Povijesni unos: ručna provjera', 'brendigo-sidrena-cijena' ); ?></span>
			</div>
			<?php endif; ?>

			<div class="brendigo-setting-row">
				<div><strong><?php echo esc_html__( 'Prikaži tooltip', 'brendigo-sidrena-cijena' ); ?></strong><small><?php echo esc_html__( 'Kupcu kratko objasnite što bazna cijena znači.', 'brendigo-sidrena-cijena' ); ?></small></div>
				<?php $this->render_toggle( 'sidrena_cijena_enable_tooltip', 'sidrena_cijena_enable_tooltip', 'yes' === $sidrena_settings->get( 'enable_tooltip' ), __( 'Tooltip', 'brendigo-sidrena-cijena' ) ); ?>
			</div>

			<label class="brendigo-field" for="sidrena_cijena_prefix">
				<span><?php echo esc_html__( 'Oznaka retka', 'brendigo-sidrena-cijena' ); ?></span>
				<input type="text" id="sidrena_cijena_prefix" name="sidrena_cijena_prefix" value="<?php echo esc_attr( $sidrena_settings->get( 'prefix' ) ); ?>" />
				<small><?php echo esc_html__( 'Preporučeno: “Bazna cijena”.', 'brendigo-sidrena-cijena' ); ?></small>
			</label>

			<div class="brendigo-field">
				<span><?php echo esc_html__( 'Format datuma', 'brendigo-sidrena-cijena' ); ?></span>
				<div class="sidrena-cijena-choices">
					<label class="sidrena-cijena-choice"><input type="radio" name="sidrena_cijena_date_style" value="parens" <?php checked( $sidrena_settings->get( 'date_style' ), 'parens' ); ?> /><span><?php echo esc_html__( 'Bazna cijena (datum)', 'brendigo-sidrena-cijena' ); ?></span></label>
					<label class="sidrena-cijena-choice"><input type="radio" name="sidrena_cijena_date_style" value="plain" <?php checked( $sidrena_settings->get( 'date_style' ), 'plain' ); ?> /><span><?php echo esc_html__( 'Bazna cijena datum', 'brendigo-sidrena-cijena' ); ?></span></label>
				</div>
				<code id="sidrena-cijena-preview"><?php echo esc_html( $sidrena_settings->get_preview_label() . $sidrena_settings->preview_separator() . '19,99 €' ); ?></code>
			</div>

			<div class="brendigo-field sidrena-cijena-tooltip-fields">
				<label for="sidrena_cijena_tooltip"><?php echo esc_html__( 'Tekst tooltipa', 'brendigo-sidrena-cijena' ); ?></label>
				<textarea rows="4" id="sidrena_cijena_tooltip" name="sidrena_cijena_tooltip"><?php echo esc_textarea( $sidrena_settings->get( 'tooltip' ) ); ?></textarea>
			</div>
		</section>

		<section class="sidrena-cijena-card brendigo-settings-card">
			<div class="sidrena-cijena-card__head brendigo-card-title">
				<span class="dashicons dashicons-calendar-alt" aria-hidden="true"></span>
				<div><h2><?php echo esc_html__( 'Referentni datumi', 'brendigo-sidrena-cijena' ); ?></h2><p><?php echo esc_html__( 'Globalni referentni datumi su zaključani i ne mijenjaju se u postavkama dodatka.', 'brendigo-sidrena-cijena' ); ?></p></div>
			</div>
			<div class="brendigo-locked-date"><span class="dashicons dashicons-lock" aria-hidden="true"></span><div><strong><?php echo esc_html( Brendigo_Sidrena_Cijena_Settings::REFERENCE_DATE_GENERAL ); ?></strong><small><?php echo esc_html__( 'ostali proizvodi i usluge', 'brendigo-sidrena-cijena' ); ?></small></div></div>
			<div class="brendigo-locked-date"><span class="dashicons dashicons-lock" aria-hidden="true"></span><div><strong><?php echo esc_html( Brendigo_Sidrena_Cijena_Settings::REFERENCE_DATE_FMCG ); ?></strong><small><?php echo esc_html__( 'hrana, piće, kozmetika i kućanske potrepštine', 'brendigo-sidrena-cijena' ); ?></small></div></div>
			<div class="brendigo-info-box"><span class="dashicons dashicons-info-outline" aria-hidden="true"></span><p><?php echo esc_html__( 'Vlastiti referentni datum unosite samo na konkretnoj novoj stavci kada je proizvod ili usluga uvedena nakon odgovarajućeg referentnog datuma. Globalni datum ostaje zaključan.', 'brendigo-sidrena-cijena' ); ?></p></div>
		</section>

		<section class="sidrena-cijena-card brendigo-settings-card">
			<div class="sidrena-cijena-card__head brendigo-card-title">
				<span class="dashicons dashicons-media-spreadsheet" aria-hidden="true"></span>
				<div><h2><?php echo esc_html__( 'Javni cjenik i podaci', 'brendigo-sidrena-cijena' ); ?></h2><p><?php echo esc_html__( 'Postavke podataka koji se objavljuju u javnom HTML/XML/CSV cjeniku.', 'brendigo-sidrena-cijena' ); ?></p></div>
			</div>
			<div class="brendigo-setting-row">
				<div><strong><?php echo esc_html__( 'Cijena za jedinicu mjere (CJM)', 'brendigo-sidrena-cijena' ); ?></strong><small><?php echo esc_html__( 'Omogućite mjernu jedinicu i pripadajući iznos kada je primjenjivo.', 'brendigo-sidrena-cijena' ); ?></small></div>
				<?php $this->render_toggle( 'sidrena_cijena_enable_cjm', 'sidrena_cijena_enable_cjm', $sidrena_settings->is_cjm_enabled(), __( 'CJM', 'brendigo-sidrena-cijena' ) ); ?>
			</div>
			<?php if ( $sidrena_woo ) : ?>
			<label class="brendigo-field" for="sidrena_cijena_brand_taxonomy"><span><?php echo esc_html__( 'Taksonomija marke', 'brendigo-sidrena-cijena' ); ?></span><select id="sidrena_cijena_brand_taxonomy" name="sidrena_cijena_brand_taxonomy"><option value=""><?php echo esc_html__( 'Bez taksonomije', 'brendigo-sidrena-cijena' ); ?></option><?php foreach ( $sidrena_settings->brand_taxonomy_choices() as $sidrena_tax_slug => $sidrena_tax_label ) : ?><option value="<?php echo esc_attr( $sidrena_tax_slug ); ?>" <?php selected( (string) $sidrena_settings->get( 'brand_taxonomy', '' ), $sidrena_tax_slug ); ?>><?php echo esc_html( $sidrena_tax_label ); ?></option><?php endforeach; ?></select></label>
			<?php endif; ?>
			<label class="brendigo-field" for="sidrena_cijena_brand_manual"><span><?php echo esc_html__( 'Ručna marka', 'brendigo-sidrena-cijena' ); ?></span><input type="text" id="sidrena_cijena_brand_manual" name="sidrena_cijena_brand_manual" value="<?php echo esc_attr( (string) $sidrena_settings->get( 'brand_manual', '' ) ); ?>" /><small><?php echo esc_html__( 'Koristi se kao rezervna vrijednost ako stavka nema vlastitu marku.', 'brendigo-sidrena-cijena' ); ?></small></label>
			<a class="brendigo-inline-link" href="<?php echo esc_url( $this->get_settings_tab_url( 'javni-xml' ) ); ?>"><span class="dashicons dashicons-external" aria-hidden="true"></span><?php echo esc_html__( 'Otvori postavke javnog cjenika i arhive', 'brendigo-sidrena-cijena' ); ?></a>
		</section>

		<section class="sidrena-cijena-card brendigo-settings-card brendigo-live-preview">
			<div class="sidrena-cijena-card__head brendigo-card-title"><span class="dashicons dashicons-visibility" aria-hidden="true"></span><div><h2><?php echo esc_html__( 'Pregled prikaza', 'brendigo-sidrena-cijena' ); ?></h2><p><?php echo esc_html__( 'Pregled izgleda bazne cijene uz proizvod.', 'brendigo-sidrena-cijena' ); ?></p></div></div>
			<div class="brendigo-preview-demo">
				<div class="brendigo-preview-demo__media"><span class="dashicons dashicons-products" aria-hidden="true"></span></div>
				<h3><?php echo esc_html__( 'Primjer proizvoda', 'brendigo-sidrena-cijena' ); ?></h3>
				<div class="brendigo-preview-price-row"><span><?php echo esc_html__( 'Važeća cijena', 'brendigo-sidrena-cijena' ); ?></span><strong>149,99 €</strong></div>
				<div class="brendigo-preview-price-row"><span><?php
				printf(
					/* translators: %s: locked general reference date. */
					esc_html__( 'Bazna cijena (%s)', 'brendigo-sidrena-cijena' ),
					esc_html( Brendigo_Sidrena_Cijena_Settings::REFERENCE_DATE_GENERAL )
				);
				?></span><strong>179,99 €</strong></div>
				<div class="brendigo-preview-note"><span class="dashicons dashicons-info-outline" aria-hidden="true"></span><span><?php echo esc_html__( 'Datum se može spremiti zasebno za svaki proizvod.', 'brendigo-sidrena-cijena' ); ?></span></div>
			</div>
		</section>
		<section class="sidrena-cijena-card brendigo-settings-card bazna-public-options">
			<div class="sidrena-cijena-card__head brendigo-card-title">
				<span class="dashicons dashicons-art" aria-hidden="true"></span>
				<div>
					<h2><?php echo esc_html__( 'Izgled javnog cjenika i arhive', 'brendigo-sidrena-cijena' ); ?></h2>
					<p><?php echo esc_html__( 'Prilagodite javni HTML prikaz bez promjene obveznog CSV/XML sadržaja.', 'brendigo-sidrena-cijena' ); ?></p>
				</div>
			</div>
			<label class="brendigo-field" for="sidrena_cijena_public_theme">
				<span><?php echo esc_html__( 'Tema', 'brendigo-sidrena-cijena' ); ?></span>
				<select name="sidrena_cijena_public_theme" id="sidrena_cijena_public_theme">
					<option value="dark" <?php selected( $sidrena_settings->get( 'public_theme', 'dark' ), 'dark' ); ?>><?php echo esc_html__( 'Tamna', 'brendigo-sidrena-cijena' ); ?></option>
					<option value="light" <?php selected( $sidrena_settings->get( 'public_theme', 'dark' ), 'light' ); ?>><?php echo esc_html__( 'Svijetla', 'brendigo-sidrena-cijena' ); ?></option>
				</select>
			</label>
			<label class="brendigo-field" for="sidrena_cijena_public_accent">
				<span><?php echo esc_html__( 'Naglasna boja', 'brendigo-sidrena-cijena' ); ?></span>
				<select name="sidrena_cijena_public_accent" id="sidrena_cijena_public_accent">
					<option value="orange" <?php selected( $sidrena_settings->get( 'public_accent', 'orange' ), 'orange' ); ?>><?php echo esc_html__( 'Narančasta · BAZNA', 'brendigo-sidrena-cijena' ); ?></option>
					<option value="blue" <?php selected( $sidrena_settings->get( 'public_accent', 'orange' ), 'blue' ); ?>><?php echo esc_html__( 'Plava', 'brendigo-sidrena-cijena' ); ?></option>
					<option value="green" <?php selected( $sidrena_settings->get( 'public_accent', 'orange' ), 'green' ); ?>><?php echo esc_html__( 'Zelena', 'brendigo-sidrena-cijena' ); ?></option>
					<option value="purple" <?php selected( $sidrena_settings->get( 'public_accent', 'orange' ), 'purple' ); ?>><?php echo esc_html__( 'Ljubičasta', 'brendigo-sidrena-cijena' ); ?></option>
				</select>
			</label>
			<label class="brendigo-field" for="sidrena_cijena_public_density">
				<span><?php echo esc_html__( 'Razmaci u prikazu', 'brendigo-sidrena-cijena' ); ?></span>
				<select name="sidrena_cijena_public_density" id="sidrena_cijena_public_density">
					<option value="comfortable" <?php selected( $sidrena_settings->get( 'public_density', 'comfortable' ), 'comfortable' ); ?>><?php echo esc_html__( 'Prozračno', 'brendigo-sidrena-cijena' ); ?></option>
					<option value="compact" <?php selected( $sidrena_settings->get( 'public_density', 'comfortable' ), 'compact' ); ?>><?php echo esc_html__( 'Kompaktno', 'brendigo-sidrena-cijena' ); ?></option>
				</select>
			</label>
			<div class="brendigo-info-box">
				<span class="dashicons dashicons-shortcode" aria-hidden="true"></span>
				<p><?php echo esc_html__( 'Umetnite u WordPress blok „Kratki kod”: ', 'brendigo-sidrena-cijena' ); ?><code>[bazna_objava]</code><br />
				<?php echo esc_html__( 'Samo cjenik:', 'brendigo-sidrena-cijena' ); ?> <code>[bazna_cjenik]</code> ·
				<?php echo esc_html__( 'Arhiva:', 'brendigo-sidrena-cijena' ); ?> <code>[bazna_arhiva]</code> ·
				<?php echo esc_html__( 'Službeni formati:', 'brendigo-sidrena-cijena' ); ?> <code>[bazna_datoteke]</code></p>
			</div>
			<p class="description"><?php echo esc_html__( 'HTML sučelje služi za pregled. Službene datoteke moraju ostati javno dostupne u CSV/XML formatu na stabilnim URL adresama, neovisno o odabranoj temi.', 'brendigo-sidrena-cijena' ); ?></p>
		</section>
	</div>

	<div class="brendigo-settings-submit">
		<?php $this->render_save_button( __( 'Spremi postavke', 'brendigo-sidrena-cijena' ) ); ?>
	</div>
</form>

<?php if ( $sidrena_woo ) : ?>
<section class="sidrena-cijena-card brendigo-tools-card">
	<div class="sidrena-cijena-card__head brendigo-card-title">
		<span class="dashicons dashicons-shield" aria-hidden="true"></span>
		<div><h2><?php echo esc_html__( 'Provjera povijesnih cijena', 'brendigo-sidrena-cijena' ); ?></h2>
		<p><?php echo esc_html__( 'Masovno kopiranje trenutačnih cijena i automatsko dodavanje datuma onemogućeno je: ne dokazuje cijenu na referentni dan.', 'brendigo-sidrena-cijena' ); ?></p></div>
	</div>
	<div class="brendigo-info-box">
		<span class="dashicons dashicons-info-outline" aria-hidden="true"></span>
		<p><?php echo esc_html__( 'Za svaku stavku utvrdite je li primjenjiv 2. 5. 2025. ili 10. 9. 2026.; koristite sačuvanu evidenciju i ručno unesite odgovarajući iznos i datum. Bazna cijena i najniža cijena u 30 dana ne smiju se automatski izvoditi iz bazne cijene.', 'brendigo-sidrena-cijena' ); ?></p>
	</div>
</section>
<?php endif; ?>

<?php if ( current_user_can( 'activate_plugins' ) ) : ?>
<section class="sidrena-cijena-card brendigo-danger-card">
	<div class="sidrena-cijena-card__head brendigo-card-title"><span class="dashicons dashicons-trash" aria-hidden="true"></span><div><h2><?php echo esc_html__( 'Podaci dodatka', 'brendigo-sidrena-cijena' ); ?></h2><p><?php echo esc_html__( 'Radnja u nastavku briše sve podatke dodatka, uključujući evidenciju povijesti cijena, i ne može se poništiti. Prije brisanja napravite sigurnosnu kopiju i provjerite obveze čuvanja evidencije.', 'brendigo-sidrena-cijena' ); ?></p></div></div>
	<button type="button" class="button sidrena-cijena-purge" id="sidrena-cijena-purge"><span class="dashicons dashicons-trash" aria-hidden="true"></span><?php echo esc_html__( 'Obriši sve podatke i deaktiviraj', 'brendigo-sidrena-cijena' ); ?></button>
	<span class="sidrena-cijena-purge-status" aria-live="polite"></span>
</section>
<?php endif; ?>
