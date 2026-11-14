package com.brendigo.bazna.data

import okhttp3.Dns
import java.net.InetAddress

/**
 * Shared DNS guard for both published catalogs and product thumbnails.
 *
 * Apply the checks to addresses actually returned to OkHttp. Never replace
 * this with a one-off DNS preflight (which is vulnerable to rebinding).
 */
internal object PublicDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        require(addresses.isNotEmpty() && addresses.all(PublicNetworkGuard::isPublic)) {
            "Povezana BAZNA domena mora imati samo javne mrežne adrese."
        }
        return addresses
    }
}
