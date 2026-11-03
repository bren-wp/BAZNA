<?php
/**
 * BAZNA WordPress license client — no external SDKs, SQL tables or credentials
 * in public endpoints. Central verification is required for global one-domain
 * uniqueness; the site's WordPress options only cache a short-lived result.
 */
defined('ABSPATH') || exit;

final class Brendigo_Sidrena_Cijena_License {
    const OPTION = 'bazna_license_state';
    const PROOF_OPTION = 'bazna_license_domain_proof';
    const CHECK_HOOK = 'bazna_license_periodic_check';
    const API = 'https://licenca.brendigo.com/licenca/api.php';
    const CHECK_INTERVAL = DAY_IN_SECONDS;
    const OFFLINE_GRACE = 3 * DAY_IN_SECONDS;

    private static $instance;

    public static function instance() {
        if (null === self::$instance) {
            self::$instance = new self();
        }
        return self::$instance;
    }

    private function __construct() {
        add_action('rest_api_init', array($this, 'register_proof'));
        add_action('admin_init', array($this, 'handle_form'), 1);
        add_action('admin_init', array($this, 'guard_locked_writes'), 2);
        add_action('init', array($this, 'schedule_check'));
        add_action(self::CHECK_HOOK, array($this, 'refresh'));
    }

    public static function site_host() {
        $host = (string) wp_parse_url(home_url('/'), PHP_URL_HOST);
        return strtolower(rtrim($host, '.'));
    }

    /**
     * A key belongs to one canonical DNS domain, not to an arbitrary domain
     * provided by an administrator. www is treated as the same website.
     */
    public static function canonical_domain($input) {
        $domain = strtolower(trim((string) $input));
        if (strlen($domain) > 253 ||
            !preg_match('/^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$/D', $domain)) {
            return '';
        }
        return preg_replace('/^www\\./', '', $domain);
    }

    /** Explicitly entered domain must identify this exact installation. */
    public static function domain_matches_site($input) {
        $claimed = self::canonical_domain($input);
        $actual = self::canonical_domain(self::site_host());
        return '' !== $claimed && '' !== $actual &&
            hash_equals($actual, $claimed);
    }

    public static function site_path() {
        // The public home URL can differ from the WordPress installation
        // directory. Use REST URL to locate the proof endpoint correctly.
        $path = (string) wp_parse_url(rest_url('brendigo-sidrena/v1/license-proof'), PHP_URL_PATH);
        $suffix = '/wp-json/brendigo-sidrena/v1/license-proof';
        if (substr($path, -strlen($suffix)) === $suffix) {
            $path = substr($path, 0, -strlen($suffix));
        }
        return '' !== $path ? trailingslashit($path) : '/';
    }

    /** Cache never grants an extra domain or indefinite offline use. */
    public static function valid() {
        $state = get_option(self::OPTION, array());
        return is_array($state) && !empty($state['active']) &&
            isset($state['host'], $state['checked']) &&
            '' !== self::canonical_domain(self::site_host()) &&
            hash_equals((string) $state['host'], self::canonical_domain(self::site_host())) &&
            (int) $state['checked'] > time() - self::OFFLINE_GRACE;
    }

    private static function encryption_key() {
        return hash('sha256', wp_salt('auth') . ':BAZNA-LICENCE-V1', true);
    }

    private static function encrypt_key($key) {
        if (!function_exists('openssl_encrypt')) {
            return '';
        }
        $nonce = random_bytes(12);
        $tag = '';
        $cipher = openssl_encrypt($key, 'aes-256-gcm', self::encryption_key(),
            OPENSSL_RAW_DATA, $nonce, $tag, '', 16);
        return false === $cipher ? '' : base64_encode($nonce . $tag . $cipher);
    }

    private static function decrypt_key($encrypted) {
        if (!is_string($encrypted) || !function_exists('openssl_decrypt')) {
            return '';
        }
        $bytes = base64_decode($encrypted, true);
        if (!is_string($bytes) || strlen($bytes) < 29) {
            return '';
        }
        $plain = openssl_decrypt(substr($bytes, 28), 'aes-256-gcm',
            self::encryption_key(), OPENSSL_RAW_DATA, substr($bytes, 0, 12),
            substr($bytes, 12, 16));
        return is_string($plain) ? $plain : '';
    }

    private static function call_service($action, $key, $challenge = '') {
        $host = self::site_host();
        $path = self::site_path();
        $restHost = self::canonical_domain((string) wp_parse_url(
            rest_url('brendigo-sidrena/v1/license-proof'), PHP_URL_HOST
        ));
        // Never prove a different hostname than the one being licensed.
        if ('' === $restHost || !hash_equals(self::canonical_domain($host), $restHost)) {
            return array('ok' => false, 'code' => 'invalid_site_domain');
        }
        if (!preg_match('/^(?:[a-z0-9-]+\\.)+[a-z]{2,63}$/D', $host) ||
            !preg_match('#^/(?:[a-zA-Z0-9_.~-]+/)*$#D', $path)) {
            return array('ok' => false, 'code' => 'invalid_site_domain');
        }
        $response = wp_remote_post(self::API, array(
            'timeout' => 12,
            'redirection' => 0,
            'sslverify' => true,
            'headers' => array('Content-Type' => 'application/json', 'Accept' => 'application/json'),
            'body' => wp_json_encode(array(
                'action' => $action,
                'key' => $key,
                'host' => $host,
                'site_path' => $path,
                'challenge' => $challenge,
            )),
        ));
        if (is_wp_error($response)) {
            return array('ok' => false, 'code' => 'service_unreachable');
        }
        $status = (int) wp_remote_retrieve_response_code($response);
        $data = json_decode((string) wp_remote_retrieve_body($response), true);
        if (!is_array($data) || !isset($data['ok']) || !is_bool($data['ok']) ||
            !isset($data['code']) || !is_string($data['code']) ||
            !in_array($status, array(200, 400, 403, 409, 429, 503), true)) {
            return array('ok' => false, 'code' => 'invalid_service_reply');
        }
        return $data;
    }

    /** Public, short-lived challenge, verified by DNS-pinned server-to-site TLS. */
    public function register_proof() {
        register_rest_route('brendigo-sidrena/v1', '/license-proof', array(
            'methods' => WP_REST_Server::READABLE,
            'permission_callback' => '__return_true',
            'callback' => array($this, 'proof'),
        ));
    }

    public function proof() {
        $proof = get_option(self::PROOF_OPTION, array());
        if (!is_array($proof) || empty($proof['value']) ||
            (int) ($proof['expires'] ?? 0) < time()) {
            return new WP_Error('bazna_proof_unavailable', 'License proof unavailable', array('status' => 404));
        }
        $reply = rest_ensure_response(array('challenge' => (string) $proof['value']));
        $reply->header('Cache-Control', 'no-store, max-age=0');
        return $reply;
    }

    public function handle_form() {
        $action = isset($_POST['bazna_license_action'])
            ? sanitize_key(wp_unslash($_POST['bazna_license_action'])) : '';
        if (!in_array($action, array('activate', 'deactivate', 'refresh'), true)) {
            return;
        }
        if (!current_user_can('manage_options')) {
            wp_die(esc_html__('Samo administrator smije upravljati licencom.', 'brendigo-sidrena-cijena'), '', array('response' => 403));
        }
        check_admin_referer('bazna_license_action', 'bazna_license_nonce');
        $state = get_option(self::OPTION, array());
        if ('activate' === $action) {
            // Fail before contacting the central service so an activation is
            // never registered if this WordPress host cannot store its key.
            if (!function_exists('openssl_encrypt') || !function_exists('openssl_decrypt')) {
                $this->redirect('encryption_unavailable');
            }
            // The administrator must explicitly enter the domain as well as
            // the key; a different website must never claim this activation.
            $domainInput = isset($_POST['bazna_license_domain']) && is_string($_POST['bazna_license_domain'])
                ? sanitize_text_field(wp_unslash($_POST['bazna_license_domain'])) : '';
            if (!self::domain_matches_site($domainInput)) {
                $this->redirect('domain_mismatch');
            }
            $key = isset($_POST['bazna_license_key'])
                ? strtoupper(trim(sanitize_text_field(wp_unslash($_POST['bazna_license_key'])))) : '';
        } else {
            $key = self::decrypt_key(is_array($state) ? ($state['encrypted_key'] ?? '') : '');
        }
        if (!preg_match('/^BAZNA-\\d{5}(?:-[A-F0-9]{4}){8}$/D', $key)) {
            $this->redirect('invalid_key');
        }
        if ('refresh' === $action) {
            $result = self::call_service('check', $key);
        } else {
            $challenge = bin2hex(random_bytes(32));
            update_option(self::PROOF_OPTION, array('value' => $challenge, 'expires' => time() + 600), false);
            try {
                $result = self::call_service($action, $key, $challenge);
            } finally {
                delete_option(self::PROOF_OPTION);
            }
        }
        if (!empty($result['ok'])) {
            if ('deactivate' === $action) {
                delete_option(self::OPTION);
                $this->redirect('deactivated');
            }
            $encrypted = self::encrypt_key($key);
            if ('' === $encrypted) {
                $this->redirect('encryption_unavailable');
            }
            $confirmedDomain = self::canonical_domain(self::site_host());
            // A successful HTTPS response must be for precisely this domain.
            if (!isset($result['domain']) || !is_string($result['domain']) ||
                !hash_equals($confirmedDomain, $result['domain'])) {
                $this->redirect('domain_mismatch');
            }
            update_option(self::OPTION, array(
                'active' => true, 'host' => $confirmedDomain,
                'checked' => time(), 'encrypted_key' => $encrypted,
            ), false);
            $this->redirect('active');
        }
        if ('refresh' === $action && is_array($state) &&
            !in_array($result['code'] ?? '', array('service_unreachable', 'license_service_unavailable'), true)) {
            $state['active'] = false;
            update_option(self::OPTION, $state, false);
        }
        $this->redirect($result['code'] ?? 'activation_failed');
    }

    public function refresh() {
        $state = get_option(self::OPTION, array());
        if (!is_array($state) || empty($state['encrypted_key'])) {
            return;
        }
        if ((int) ($state['checked'] ?? 0) > time() - self::CHECK_INTERVAL) {
            return;
        }
        $key = self::decrypt_key($state['encrypted_key']);
        if ('' === $key) {
            $state['active'] = false;
            update_option(self::OPTION, $state, false);
            return;
        }
        $result = self::call_service('check', $key);
        if (!empty($result['ok'])) {
            $state['active'] = true;
            $state['checked'] = time();
        } elseif (!in_array($result['code'] ?? '', array('service_unreachable', 'license_service_unavailable'), true)) {
            $state['active'] = false;
        }
        update_option(self::OPTION, $state, false);
    }

    public function schedule_check() {
        if (!wp_next_scheduled(self::CHECK_HOOK)) {
            wp_schedule_event(time() + HOUR_IN_SECONDS, 'hourly', self::CHECK_HOOK);
        }
    }

    /** Do not allow direct POST/AJAX/CLI writes to bypass hidden admin tabs. */
    public function guard_locked_writes() {
        if (self::valid()) {
            return;
        }
        $postAction = isset($_POST['sidrena_cijena_action'])
            ? sanitize_key(wp_unslash($_POST['sidrena_cijena_action'])) : '';
        $ajaxAction = isset($_REQUEST['action'])
            ? sanitize_key(wp_unslash($_REQUEST['action'])) : '';
        $blocked = $postAction !== '' || preg_match('/^sidrena_cijena_(?:import|snapshot)/', $ajaxAction);
        if ($blocked) {
            wp_die(esc_html__('Za uređivanje i izvoz potrebna je aktivna BAZNA licenca.', 'brendigo-sidrena-cijena'), '', array('response' => 403));
        }
        // Deliberately leave data erasure accessible to administrators.
    }

    private function redirect($code) {
        $allowed = array('active', 'deactivated', 'invalid_key', 'invalid_site_domain', 'domain_mismatch',
            'domain_proof_failed', 'already_used_on_another_domain',
            'not_activated_for_domain', 'service_unreachable', 'invalid_service_reply',
            'license_service_unavailable', 'encryption_unavailable',
            'rate_limit_exceeded', 'server_not_provisioned', 'activation_failed');
        $safe = in_array($code, $allowed, true) ? $code : 'activation_failed';
        wp_safe_redirect(add_query_arg(array(
            'page' => 'brendigo-sidrena-cijena', 'tab' => 'licenca',
            'bazna_license_status' => $safe,
        ), admin_url('admin.php')));
        exit;
    }
}
