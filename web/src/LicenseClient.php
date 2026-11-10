<?php
/** BAZNA reusable site-license client for plain PHP and Joomla. */
declare(strict_types=1);
namespace Brendigo\Bazna;
use RuntimeException;

final class LicenseClient
{
    private const API = 'https://licenca.brendigo.com/licenca/api.php';
    private const GRACE_SECONDS = 259200;

    public function __construct(private readonly Core $core, private readonly string $secret)
    {
        if (!preg_match('/^[a-fA-F0-9]{64}$/D', $secret)) {
            throw new RuntimeException('BAZNA_APP_KEY mora imati 32 nasumična bajta zapisana u hex formatu.');
        }
    }

    public function state(): array { return $this->core->read('license.json'); }

    public function active(): bool
    {
        $s = $this->state();
        return ($s['active'] ?? false) === true &&
            ($s['host'] ?? '') === $this->domain() &&
            (int) ($s['checked'] ?? 0) > time() - self::GRACE_SECONDS;
    }

    public function domain(): string
    {
        $host = strtolower(rtrim($this->core->host(), '.'));
        return (string) preg_replace('/^www\./', '', $host);
    }

    private function cryptoKey(): string { return hash('sha256', hex2bin($this->secret) . ':BAZNA-LICENSE-WEB-V1', true); }

    private function encrypt(string $plain): string
    {
        $nonce = random_bytes(12);
        $tag = '';
        $ciphertext = openssl_encrypt($plain, 'aes-256-gcm', $this->cryptoKey(),
            OPENSSL_RAW_DATA, $nonce, $tag, '', 16);
        if ($ciphertext === false) throw new RuntimeException('Nije moguće zaštititi licencni ključ.');
        return base64_encode($nonce . $tag . $ciphertext);
    }

    private function decrypt(string $encoded): string
    {
        $raw = base64_decode($encoded, true);
        if ($raw === false || strlen($raw) < 29) return '';
        $plain = openssl_decrypt(substr($raw, 28), 'aes-256-gcm', $this->cryptoKey(),
            OPENSSL_RAW_DATA, substr($raw, 0, 12), substr($raw, 12, 16));
        return is_string($plain) ? $plain : '';
    }

    public function proof(): ?string
    {
        $row = $this->core->read('proof.json');
        return (int) ($row['expires'] ?? 0) >= time() &&
            is_string($row['challenge'] ?? null) ? $row['challenge'] : null;
    }

    private function request(string $action, string $key, string $challenge = ''): array
    {
        if (!function_exists('curl_init')) throw new RuntimeException('PHP cURL je obvezan za aktivaciju licence.');
        $post = json_encode([
            'action' => $action, 'key' => $key, 'host' => $this->domain(),
            'site_path' => $this->core->basePath(), 'challenge' => $challenge,
        ], JSON_THROW_ON_ERROR);
        $curl = curl_init(self::API);
        if ($curl === false) throw new RuntimeException('Povezivanje s licencnim servisom nije dostupno.');
        curl_setopt_array($curl, [
            CURLOPT_POST => true, CURLOPT_POSTFIELDS => $post,
            CURLOPT_HTTPHEADER => ['Content-Type: application/json', 'Accept: application/json'],
            CURLOPT_RETURNTRANSFER => true, CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_SSL_VERIFYHOST => 2, CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_CONNECTTIMEOUT => 5, CURLOPT_TIMEOUT => 12,
            CURLOPT_MAXREDIRS => 0,
        ]);
        try {
            $reply = curl_exec($curl);
            $status = (int) curl_getinfo($curl, CURLINFO_HTTP_CODE);
        } finally { curl_close($curl); }
        $data = is_string($reply) && strlen($reply) <= 4096 ? json_decode($reply, true) : null;
        if (!is_array($data) || !isset($data['ok'], $data['code']) ||
            !is_bool($data['ok']) || !is_string($data['code']) ||
            !in_array($status, [200,400,403,409,429,503], true)) {
            return ['ok' => false, 'code' => 'temporary_license_error'];
        }
        return $data;
    }

    public function activate(string $key): array
    {
        $key = strtoupper(trim($key));
        if (!preg_match('/^BAZNA-\d{5}(?:-[A-F0-9]{4}){8}$/D', $key)) {
            return ['ok' => false, 'code' => 'invalid_key'];
        }
        $token = bin2hex(random_bytes(32));
        $this->core->write('proof.json', ['challenge' => $token, 'expires' => time() + 600]);
        try {
            $result = $this->request('activate', $key, $token);
        } finally {
            $this->core->write('proof.json', []);
        }
        if (($result['ok'] ?? false) === true && ($result['code'] ?? '') === 'active' &&
            hash_equals($this->domain(), (string) ($result['domain'] ?? ''))) {
            $this->core->write('license.json', [
                'active' => true, 'host' => $this->domain(),
                'key' => $this->encrypt($key), 'checked' => time(),
            ]);
        }
        return $result;
    }

    public function refresh(): array
    {
        $current = $this->state();
        $key = $this->decrypt((string) ($current['key'] ?? ''));
        if ($key === '') return ['ok' => false, 'code' => 'not_activated'];
        $result = $this->request('check', $key);
        if (($result['ok'] ?? false) === true && ($result['code'] ?? '') === 'active' &&
            hash_equals($this->domain(), (string) ($result['domain'] ?? ''))) {
            $current['active'] = true;
            $current['checked'] = time();
            $this->core->write('license.json', $current);
        } elseif (in_array($result['code'] ?? '', ['invalid_key', 'not_activated_for_domain', 'already_used_on_another_domain'], true)) {
            // Authoritative invalidation only; network errors respect grace.
            $current['active'] = false;
            $this->core->write('license.json', $current);
        }
        return $result;
    }

    public function deactivate(): array
    {
        $current = $this->state();
        $key = $this->decrypt((string) ($current['key'] ?? ''));
        if ($key === '') return ['ok' => false, 'code' => 'not_activated'];
        $token = bin2hex(random_bytes(32));
        $this->core->write('proof.json', ['challenge' => $token, 'expires' => time() + 600]);
        try { $result = $this->request('deactivate', $key, $token); }
        finally { $this->core->write('proof.json', []); }
        if (($result['ok'] ?? false) === true && ($result['code'] ?? '') === 'deactivated') {
            $this->core->write('license.json', []);
        }
        return $result;
    }
}
