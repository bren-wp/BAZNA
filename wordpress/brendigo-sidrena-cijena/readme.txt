=== BAZNA – Bazne cijene i cjenici ===
Contributors: brendigo
Tags: reference-prices, price-list, csv, croatia, woocommerce
Requires at least: 6.6
Tested up to: 7.1
Requires PHP: 7.4
Stable tag: 1.5.18
License: GPLv2 or later
License URI: https://www.gnu.org/licenses/gpl-2.0.html

BAZNA by Brendigo: bazne cijene i datumi, javni HTML cjenici te CSV/XML arhiva za WordPress i WooCommerce.

== Description ==

**BAZNA brings reference prices, transparent public pricelists and dated archives together in one WordPress dashboard.** Built for WooCommerce stores and independent WordPress catalogs, without adding an external account or mandatory hosted service.

= Publish with clarity =

* **Reference prices with dates** - keep reference values and their applicable dates visible alongside current prices.
* **Public, readable pricelists** - publish HTML pages and share XML/CSV exports with customers and search engines.
* **A lasting archive** - preserve published pricelists for future consultation and downloads.
* **WordPress or WooCommerce** - manage products, variations, services and multiple locations from familiar controls.
* **Control and visibility** - use role-protected settings, an administrator audit trail and scheduled publication.

The plugin does not invent historical prices or certify a shop's legal compliance. Store owners are responsible for entering accurate source data, testing published outputs and checking applicable legal requirements.

= Designed for your workflow =


Brendigo Bazne cijene provides one interface for managing reference-price data and publishing a public pricelist.

Key features:

* Reads products and variations directly from WooCommerce without duplicating the catalog.
* Bulk editing of reference price, reference date, barcode and public-pricelist visibility.
* Standalone manual products and services when WooCommerce is not used, with individual price cards and observed sales-price history.
* Administrator-only local audit trail for reference-price edits (time, operator, old/new value); no external analytics.
* Separate [sidrena_najniza_prije_snizenja id="d7" datum="2026-10-09"] for the recorded 30-day window before a specific sale. Incomplete history is labeled as such.
* Public HTML pricelist and XML/CSV output generated from the same saved data.
* Public archive of previously generated pricelists.
* Multiple shop locations with separate public outputs.
* CSV/XML import and export, unit-of-measure price fields and barcode support for standalone items as well as WooCommerce products.
* WPML, Polylang and common WooCommerce theme/builder compatibility.
* Automatic publication after relevant changes plus a configurable daily publication time.
* Recovery checks for missed or failed background publications.
* WP-CLI generation and health commands on supported hosting environments.
* Observed selling-price history for WooCommerce products/variations and standalone products/services, with an incomplete-30-day-history indication.
* Email alerts when scheduled publication fails or is missed, with a 12-hour per-error throttle.
* Separate XML, CSV and HTML feeds for goods and services, including multiple outlet feeds and 30-day dated archives.
* Read-only REST API for discovering current pricelists and their archives.
* Cart/Checkout Blocks reference-price display via WooCommerce Store API extension data.
* Best-effort missed-publication recovery on public visits when built-in WP-Cron is disabled; external host scheduling remains recommended.

New installations default to a daily publication time of 07:00 in the WordPress site timezone. The time is configurable. A scheduled publication updates the public HTML page and configured XML/CSV file together and then stores the published version in the archive.

Reference prices and dates are entered or imported by the site administrator. The plugin does not infer historical prices.

Documentation: https://brendigo.com/

== Installation ==

After activation, open the **Upute** (Guide) tab inside Brendigo Bazne cijene for a step-by-step onboarding guide in Croatian: separate instructions for standalone WordPress and WooCommerce, publishing, imports, public shortcodes, and troubleshooting.


1. Install the ZIP from WordPress Plugins > Add New, or upload the unmodified BAZNA plugin folder through your hosting file manager.
2. Activate **BAZNA**.
3. Open WooCommerce > BAZNA. Without WooCommerce, use the standalone BAZNA menu.
4. Open **Products** to enter reference prices and dates.
5. Open **Public pricelist** to choose the daily publication time and publish the first version.

== WooCommerce catalog workflow ==

The Products tab reads WooCommerce products directly. Product name, SKU and current price remain linked to the original product; the plugin stores only its own reference-price fields and related public-pricelist settings.

Large catalogs are paginated, and variations are grouped with their parent products.

== Standalone WordPress without WooCommerce ==

WooCommerce is optional. Activate the plugin in a standard WordPress installation and use the **Brendigo Bazne cijene** menu. Add manual products and services in the Products tab, provide the current price, reference price and date, then publish the HTML/XML/CSV pricelist. No checkout or online payment module is added to WordPress by this plugin.

Use `[bazna-stavka id="123"]` for a named card showing the current and reference price, `[bazna-cijena id="123"]` for the reference price, `[bazna-povijest-cijena id="123"]` for observed price history, and `[bazna-cjenik]` for the entire pricelist. When WooCommerce is also active, prefix the manual row ID with `d` for shortcodes that may otherwise resolve to a WooCommerce product: `id="d123"`. Individual manual item cards always use the manual catalog.

Price history starts when tracking is installed or a product is first seen; it does not backdate or establish the legally required historical reference price. Daily publishing uses the WordPress event scheduler and an on-visit recovery fallback; for predictable timing even on low-traffic sites configure a server-side cron.

== Scheduled publication ==

Daily publication follows the WordPress site timezone and is recalculated by local calendar date so daylight-saving changes do not shift the configured local time. A successful run updates the public HTML pricelist, the selected XML/CSV live file, and the archive entry.

WordPress WP-Cron is traffic-driven. If publication must run very close to an exact minute, configure the hosting scheduler to trigger WordPress cron regularly. Automatic maintenance also checks for missed publication slots and can queue recovery.

== Shortcodes ==

`[bazna-cijena]` - reference price for the current product.
`[bazna-cijena id="123"]` - reference price for a selected product or manual catalog row.
`[bazna-cjenik]` - public pricelist table.
`[bazna-arhiva]` - public pricelist archive.
`[bazna-stavka id="123"]` - standalone product or service card, no WooCommerce needed.
`[bazna-povijest-cijena id="123"]` - observed price history in standalone mode; with WooCommerce use `d123` for a manual row.

Legacy shortcode aliases remain available for backward compatibility.

== WP-CLI ==

`wp bazna regenerate` regenerates and archives the public pricelist.

`wp bazna health` reports publication schedule and automation state.

`wp bazna fill` can fill empty reference-price fields from regular prices when an administrator intentionally chooses that workflow.

== Frequently Asked Questions ==

= Does the plugin copy my WooCommerce products into another catalog? =

No. The WooCommerce catalog editor reads existing products and variations directly and stores only plugin-specific fields on those records.

= What is published at the selected daily time? =

The public HTML pricelist and selected XML/CSV live file are generated together, followed by the archive entry.

= Can I change 07:00? =

Yes. The daily publication time is configurable in the Public pricelist tab.

= Does the plugin send catalog data to an external service? =

No. Catalog processing, exports and the administrator guide run on your WordPress site. The private distribution uses a separate licensing endpoint only for license activation and periodic verification, not for processing catalog prices.

= What information does the private license service receive? =

When the administrator explicitly activates a license, the private distribution sends the entered license key and the site's canonical domain over HTTPS to https://licenca.brendigo.com for one-domain registration and verification. Periodic license checks follow. Product records, price histories and catalog contents are not sent to this service. The independent WordPress.org candidate distribution does not include this license client and does not require a key.

== Upgrade Notice ==

= 1.5.0 =
More translated interface texts for 20 languages. Untranslated texts still fall back to Croatian. No migration required.


= 1.4.7 =
More translations for every previously incomplete locale, particularly error handling and public pricelists. No database migration.


= 1.4.6 =
Adds 1,400 locale translations for the core interface and import/export error messages. No database migration.


= 1.4.5 =
Completes English translations and expands Bosnian and Serbian; other locales remain partial. No data migration.

= 1.4.4 =
Adds multilingual catalogs and fixes default prefix and locale handling. No database migration.

= 1.4.3 =
Hardens outlet and service saves, protects imported price data with server-side preflight verification, and optimizes multi-outlet processing. Back up your database before upgrading.

= 1.4.2 =
Adds in-plugin beginner instructions for standalone WordPress and WooCommerce, with direct navigation to existing features. No catalog or database changes.


= 1.4.1 =
Improves Plugin Check compatibility, SQL safety, and history performance. Keeps existing prices, feeds and both operating modes.

= 1.3.2 =
Improves barcode handling, CSV/XML imports and standalone administration. Preserves existing pricing data and URLs.

= 1.3.1 =
Adds standalone WordPress price-history tracking and an individual manual-item shortcode, hardens malformed JSON handling, makes catalog IDs monotonic, improves cleanup and privacy controls. WooCommerce stays optional. Back up your database and generated pricelists before updating.

= 1.3.0 =
Adds observed price history, publication alerts, separate product/service feeds, and REST discovery. Back up data before updating.

= 1.2.16 =
Fixes WordPress.org i18n translator-comment errors and malformed admin sprintf placeholders that could append internal text to publication, pending-change, and pagination labels.

== Changelog ==

= 1.5.18 =
* Usklađen javni status objave svih poslovnica u REST v1 i Androidu. Pravna revizija od 14. 11. 2026. bez automatskog proglašavanja nacrta pravilnika važećim.


= 1.5.17 =
* Android BAZNA package changed to com.brendigo.bazna for a new Play listing; WordPress public API and stored price data remain unchanged.


= 1.5.16 =
* Audit public BAZNA graphics and all user-visible brand labels on Android, WordPress, Web PHP, Joomla and Shopify.
* Correct the WordPress version history and legacy package names in public release documentation.
* Keep existing WordPress database keys and REST aliases and the Android application identity for uninterrupted upgrades.
* Strengthen all-platform production-readiness checks without inferring historical prices or changing stored evidence.

= 1.5.15 =
* Improve BAZNA integration parity across standalone PHP, Joomla, Shopify and Android while preserving the original public WordPress REST v1 endpoints and published price history.
* Align legal metadata, reference dates and public licensing status checks across supported BAZNA platforms.
* Preserve compatibility with existing WordPress installations, WooCommerce product metadata, shortcodes and published CSV/XML feeds.
* WordPress.org review candidate continues to ship with local features unlocked; the private licensed distribution remains separate.

= 1.5.14 =
* Improve BAZNA integration parity across standalone PHP, Joomla, Shopify and Android while preserving existing WordPress REST v1 endpoints and historical price records.
* Align legal metadata, reference dates and public licensing status checks across supported BAZNA platforms.
* Preserve compatibility with existing WordPress installations, WooCommerce product metadata, shortcodes and published CSV/XML feeds.
* Keep the WordPress.org review candidate with local features unlocked and the private licensed distribution separate.

= 1.5.13 =
* Keep already published WooCommerce reference prices visible in theme/builder layouts and cart/checkout blocks even when the admin license is inactive.
* Preserve the bounded offline license grace during HTTP 429, temporary service failure or malformed network response; revoke only an explicit invalid-key/domain decision.
* Restrict Woo product metadata editing and REST access until BAZNA license activation without deleting saved prices.
* Android accepts bare WordPress domains and adds HTTPS automatically, retaining same-origin and safe-DNS checks.
* Add license-server CLI preflight to the complete upload package, with extra Android/WordPress regression tests.
* Retain legal distinction between actual, reference and 30-day lowest prices and publicly readable CSV/XML feeds.

= 1.5.12 =
* Publish a deployable standalone BAZNA PHP/JSON license-server archive, with protected paths, Nginx/Apache instructions and private-config overlay.
* License API readiness now refuses to claim success if the required master secret, PHP modules or private JSON storage are unavailable.
* Keep a permanent one-domain registration, reject unsafe JSON registry states and never publish keys to Android REST clients.
* Android now checks the optional non-sensitive license status and continues displaying published prices even when the admin license is inactive.
* Add CI tests, reproducible checksums, 2026 Croatian legal notes and hardened infrastructure documentation.

= 1.5.11 =
* Rebrand the user-facing system as BAZNA — Bazne cijene, including WordPress administration and Android/Windows interfaces.
* Keep the existing WordPress plugin folder, REST v1 routes, database options and WooCommerce meta for in-place updates.
* Add bazna_cijena, bazna_cjenik and bazna_arhiva shortcode aliases while preserving legacy shortcodes.
* Migrate only factory default reference-price labels and tooltips; preserve merchant customizations.
* Introduce a BAZNA vector icon and legacy-install-safe desktop branding.
* Add compatibility and regression checks, legal terminology review and staged migration documentation.

= 1.5.10 =
* Prevent stale public REST publication status and 302 feed redirects with explicit no-store cache policy.
* Harden Android CSV handling against duplicate or ambiguous price headers and malformed row widths.
* Validate public read-only Android connection capabilities in the official WordPress discovery contract.
* Request fresh published data and reject incomplete HTTP responses; add contract regression tests.
* Preserve existing historical prices, public CSV/XML columns, archives and anonymous read-only REST v1 routes.

= 1.5.9 =
* Improve Android HTTPS linking on WordPress installations using plain or pretty REST permalinks, without allowing cross-origin or private network access.
* Add explicit, anonymous, read-only Android connection details to the public v1 discovery API; all published CSV/XML and archive routes remain unchanged.
* Remove unreachable legacy price-snapshot routines that must not infer historic reference prices.
* Expand WordPress/Android API contract, security and legal regression checks.

= 1.5.8 =
* Improve responsive editor headings and action buttons on mobile and tablet WordPress admin screens.
* Extend Chrome UI regression checks for wrapped catalog headings without modifying existing product or price data.
* Keep public REST v1, archive, CSV/XML and legal reference prices compatible with prior versions.

= 1.5.7 =
* Improve mobile and tablet editing ergonomics in the wide catalog without changing catalog data or columns.
* Use minimum 44px controls inside the horizontally scrollable price table.
* Expand browser regression checks for editable rows and ensure all mobile failures are reported independently.
* Preserve REST v1, historical anchor prices and existing CSV/XML formats.

= 1.5.6 =
* Fix narrow WordPress administrator navigation so all BAZNA tabs are visible in two columns without unwanted horizontal clipping.
* Increase navigation, search, select and save controls to 44px minimum touch height on mobile.
* Add 13-viewport browser regression checks for navigation layout, tap targets and viewport clipping.
* Preserve existing prices, historical records, public CSV/XML formats and REST v1 API.

= 1.5.5 =
* Improve mobile pricelist card wrapping and visual consistency with BAZNA Android.
* Preserve existing CSV/XML fields, prices, historical dates and REST v1 compatibility.
* Refine WordPress mobile interface regression checks without external runtime dependencies.

= 1.5.4 =
* Added public read-only CSV feed availability flags in the existing BAZNA REST API, including separate business outlet statuses, to prevent misleading Android loading states.
* Added optional WooCommerce product thumbnails to the public product CSV/XML feed without changing the required price, date or service columns.
* Added a WordPress dashboard connection panel for the BAZNA Android application, with copyable HTTPS address and actual published-feed readiness.
* Aligned BAZNA admin and public price-list colors with the Android brand; no new third-party dependencies.
* Kept API version 1 and compatibility with older Android clients. Changes are prepared in a PR and require deployment to become available.

= 1.5.3 =
* Separate the 30-day sale baseline from documented historical reference prices in the public legal metadata.
* Document the effective date of NN 105/2026 and pending 2026 rulebook consultation without changing existing public CSV/XML schemas or saved prices.

= 1.5.2 =
* Published machine-readable legal guidance for the 2026 reference-price dates, effective date and 30-day public archive requirement in the existing read-only REST catalog discovery endpoint.
* Kept CSV/XML publication schemas backward-compatible with version 1.5.0 and made the public API identify the exact plugin version.
* Preserved the administrator's responsibility to assign the appropriate historical reference date; no automatic invention of prices or inference of product categories.

= 1.5.0 =
* Expanded incomplete translations across 20 languages; these 20 remain partial.
* Fixed Serbian Latin translation filenames to `sr_RS_latin` and regenerated all compiled locale catalogs.
* Removed manual translation loading discouraged for WordPress.org-hosted plugins.
* Added BAZNA brand symbol to the WordPress admin and prepared separate WordPress.org icon/banner assets.
* Kept Croatian as the source language and reference-price logic unchanged.

= 1.4.4 =
* Added 27 partial language packs, POT template, and corrected locale selection and default label.
* Preserved Croatian as source and unchanged reference-price business logic.

= 1.4.3 =
* Hardened outlet JSON validation and prevented destructive changes on failed writes.
* Kept outlet and manual service identifiers stable across deletion.
* Required matching, successful server-side import preflight before applying CSV/XML/XLSX import mappings.
* Reduced unnecessary repeated catalog scans for multi-outlet generation.
* Removed a duplicate import-column alias; retained existing public APIs and WordPress/WooCommerce modes.

= 1.4.2 =
* Added a fully local, access-controlled Upute admin tab with beginner onboarding guides for two operating modes and common setup tasks.
* Linked help from the admin header, plugin action links and overview dashboard.
* Kept current public price outputs and WordPress.org distribution clean of extra markdown files.

= 1.4.1 =
* Fixed WordPress.org Plugin Check errors for options, temporary XLSX files and product query filters.
* Improved plugin-owned SQL safety and reduced repeat database reads while retaining up-to-date administrator audit information.
* Removed internal development documentation from the distributable package.

= 1.3.3 =
* Added an administrator-only local change log for reference price edits in manual catalogs and plugin-managed WooCommerce writes, including WooCommerce CSV imports after successful saves.
* Added `[sidrena_najniza_prije_snizenja id="d7" datum="2026-10-09"]` for a dated, separate pre-discount 30-day observation. Data are marked incomplete when no baseline proves all 30 days.
* Preserved recent 31-day archival data even when archives contain more than 30 files.
* Improved protection against incorrectly logging unsuccessful saves, with regression tests for standalone and WooCommerce CSV integration.

= 1.3.1 =
* Fully support observing manual product/service price changes without WooCommerce; protect history from WooCommerce ID collisions.
* Add `[bazna-stavka id="123"]` for a lightweight named manual product/service card.
* Prevent malformed or truncated catalog JSON from wiping stored data, strictly validate item IDs, and avoid reusing automatic row IDs after deletion.
* Improve manual-row lookup with an in-request index and prevent accidental repeated unslashing of stored text.
* Disable automatic historical reference-price autofill on new installs only; keep existing saved preferences unchanged.
* Maintain standalone history independently of WooCommerce; explicitly purge history only on administrator-confirmed full data wipe.
* Keep original prices, dates, WooCommerce functionality, exports, archives, public URLs and existing shortcodes compatible.

= 1.3.0 =
* Record observed WooCommerce product and variation price changes; calculate the lowest observed price within the 30-day window, marking incomplete history.
* Capture current baseline prices for existing products incrementally and keep 400 days of price observations.
* Send rate-limited failure and missed-publication email alerts to the designated address or site administrator.
* Publish separate goods/services XML, CSV and HTML alongside unchanged existing combined live feeds, with per-outlet exports and dated archives.
* Provide a read-only REST API for published feeds and separate archives.
* Integrate reference-price display into the WooCommerce Store API and Cart/Checkout Blocks.
* Add best-effort overdue publication repair when WP-Cron is disabled, plus server-side cron configuration guidance.
* Preserve reference-price dates, existing option keys, product metadata, shortcodes, archive schema and slug.

= 1.2.16 =
* Moved translator comments directly above all newly reported placeholder translation calls so WordPress i18n checks can associate them correctly.
* Fixed malformed numbered sprintf placeholders in publication status, pending-change status, and WooCommerce pagination labels.
* Re-audited translatable placeholder strings and release metadata.

= 1.2.15 =
* Added a deterministic class-map autoloader so plugin classes load only when a request actually needs them.
* Removed redundant manual class bootstrap branches and more unused localized JavaScript configuration.
* Centralized locked reference-date values so product fields, admin guidance and import templates cannot drift from the configured constants.
* Reduced unnecessary frontend stylesheet work when automatic display is disabled or standalone pages do not render reference prices.
* Deferred the storefront compatibility script and removed a repeated full-document legacy-tooltip cleanup from every injected price node.
* Removed the current tooltip info icon markup/CSS while preserving keyboard-accessible tooltip behavior and legacy cached-markup cleanup.
* Refined admin switch states and corrected reduced-motion loading-indicator behavior.
* Sanitized and bounded exception messages before they are passed to logging integrations.

= 1.2.14 =
* Centralized plugin-owned filesystem access and removed duplicated filesystem initialization.
* Lazy-loaded dashboard/import/editor code outside normal storefront requests.
* Removed verified duplicate forwarding methods, shared manual-row helper implementations, obsolete CLI fallback code, unused CSS variables and empty package directories.
* Consolidated repeated standalone public-page HTML shell/back-link output and uses the WordPress site language/direction.
* Moved older release history to `changelog.txt` and reduced readme size for the WordPress.org parser.

For older release history, see `changelog.txt`.
