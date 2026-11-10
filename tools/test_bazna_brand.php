<?php
/** Verify upgrade from legacy factory labels without overwriting custom copy. */
define('ABSPATH', __DIR__ . '/');
$GLOBALS['bazna_test_options'] = array();
function get_option($key, $default = false) {
    return array_key_exists($key, $GLOBALS['bazna_test_options'])
        ? $GLOBALS['bazna_test_options'][$key] : $default;
}
function update_option($key, $value) {
    $GLOBALS['bazna_test_options'][$key] = $value;
    return true;
}
function wp_parse_args($args, $defaults = array()) {
    return array_merge((array) $defaults, (array) $args);
}
function bazna_expect($condition, $message) {
    if (!$condition) {
        fwrite(STDERR, 'FAIL: ' . $message . PHP_EOL);
        exit(1);
    }
}
require_once __DIR__ . '/../wordpress/brendigo-sidrena-cijena/includes/class-settings.php';
$class = 'Brendigo_Sidrena_Cijena_Settings';
bazna_expect($class::OPTION_KEY === 'sidrena_cijena_settings', 'WordPress option remains compatible');
bazna_expect($class::DEFAULT_PREFIX === 'Bazna cijena', 'New factory label');
$GLOBALS['bazna_test_options'][$class::OPTION_KEY] = array(
    'prefix' => 'Sidrena cijena',
    'tooltip' => $class::PREVIOUS_BRAND_TOOLTIP,
    'default_date' => $class::REFERENCE_DATE_GENERAL,
    'i18n' => array(
        'prefix' => array('en' => 'Historical price', 'hr' => 'Sidrena cijena'),
        'tooltip' => array('hr' => $class::PREVIOUS_BRAND_TOOLTIP),
    ),
);
$instance = new $class();
$state = $instance->all();
bazna_expect($state['prefix'] === 'Bazna cijena', 'Old factory label automatically migrated');
bazna_expect($state['tooltip'] === $class::DEFAULT_TOOLTIP, 'Old factory tooltip migrated');
bazna_expect($state['i18n']['prefix']['en'] === 'Historical price', 'Custom translation retained');
bazna_expect($state['i18n']['prefix']['hr'] === $class::DEFAULT_PREFIX,
    'Legacy translation for BAZNA prefix migrated');
bazna_expect($state['i18n']['tooltip']['hr'] === $class::DEFAULT_TOOLTIP,
    'Legacy translation for BAZNA tooltip migrated');
bazna_expect($GLOBALS['bazna_test_options'][$class::OPTION_KEY]['prefix'] === 'Bazna cijena', 'Persist migration');

$GLOBALS['bazna_test_options'][$class::OPTION_KEY] = array(
    'prefix' => 'Povijesna cijena prema evidenciji',
    'tooltip' => 'Vlastita obavijest za kupce.',
    'default_date' => $class::REFERENCE_DATE_GENERAL,
);
$another = new $class();
$custom = $another->all();
bazna_expect($custom['prefix'] === 'Povijesna cijena prema evidenciji', 'Merchant custom label preserved');
bazna_expect($custom['tooltip'] === 'Vlastita obavijest za kupce.', 'Merchant custom tooltip preserved');

$root = dirname(__DIR__);
$front = file_get_contents($root . '/wordpress/brendigo-sidrena-cijena/includes/class-frontend.php');
foreach (array('bazna_cijena', 'bazna-cijena', 'bazna_cjenik', 'bazna-cjenik',
    'bazna_arhiva', 'bazna-arhiva', 'bazna-stavka', 'bazna_stavka',
    'bazna_proizvodi', 'bazna_usluge',
    'sidrena-cijena', 'sidrena-cjenik', 'sidrena-arhiva') as $shortcode) {
    bazna_expect(strpos($front, "add_shortcode( '" . $shortcode . "'") !== false,
        'Both brand and legacy shortcode ' . $shortcode);
}
$history = file_get_contents($root . '/wordpress/brendigo-sidrena-cijena/includes/class-price-history.php');
foreach (array('bazna-povijest-cijena', 'bazna_povijest_cijena',
    'bazna_najniza_prije_snizenja', 'bazna_graf_cijena', 'bazna-graf-cijena') as $shortcode) {
    bazna_expect(strpos($history, "add_shortcode( '" . $shortcode . "'") !== false,
        'BAZNA historical shortcode ' . $shortcode);
}
$cli = file_get_contents($root . '/wordpress/brendigo-sidrena-cijena/includes/class-plugin.php');
bazna_expect(strpos($cli, "add_command( 'bazna'") !== false, 'BAZNA WP-CLI entry');
$adminJs = file_get_contents($root . '/wordpress/brendigo-sidrena-cijena/assets/js/admin.js');
bazna_expect(strpos($adminJs, 'prefix = "Bazna cijena"') !== false, 'JavaScript BAZNA prefix preview');
$catalogJs = file_get_contents($root . '/wordpress/brendigo-sidrena-cijena/assets/js/admin-catalog.js');
bazna_expect(strpos($catalogJs, '[bazna-cijena id=') !== false, 'BAZNA shortcodes copied from catalog');
$api = file_get_contents($root . '/wordpress/brendigo-sidrena-cijena/includes/class-public-api.php');
bazna_expect(strpos($api, "'brendigo-sidrena/v1'") !== false, 'Legacy REST v1 contract preserved');
bazna_expect(strpos($api, "'name' => 'BAZNA'") !== false, 'Public Android-facing brand updated');
echo "PASS: BAZNA factory migration, legacy WP options, shortcodes and public API\n";
