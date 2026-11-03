<?php
/** Copy to config.php ONLY in your private deployment, NEVER commit the real secret. */
defined('BAZNA_LICENSE_INTERNAL') || exit;
return array(
    // 32 cryptographically random bytes encoded as 64 lowercase hex chars.
    'master_secret_hex' => 'REPLACE_WITH_64_HEX_CHARACTERS',
    // JSON is stored OUTSIDE the public web root. Set an absolute writable path.
    // Omit storage_dir to use the safe server default outside the web root.
);
