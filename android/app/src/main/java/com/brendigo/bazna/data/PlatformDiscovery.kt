package com.brendigo.bazna.data

import java.net.URI

/**
 * Public read-only BAZNA v1 discovery across all supported CMS platforms.
 * Preserve WordPress pretty/plain aliases first for existing installations.
 * Every candidate is same-origin HTTPS via SiteUrl.endpoint/approved.
 * Never add third-party fallback hosts or tokens.
 */
internal fun platformDiscoveryCandidates(site: SiteUrl): List<URI> = listOf(
    site.endpoint("wp-json/brendigo-bazna/v1/cjenici"),
    site.queryRestEndpoint("brendigo-bazna/v1/cjenici"),
    site.endpoint("wp-json/brendigo-sidrena/v1/cjenici"),
    site.queryRestEndpoint("brendigo-sidrena/v1/cjenici"),
    site.endpoint("bazna/v1/cjenici"),           // standalone PHP / Joomla
    site.endpoint("apps/bazna/v1/cjenici"),      // Shopify default app proxy
    site.endpoint("v1/cjenici"),                 // explicit Shopify app proxy base
).distinct()
