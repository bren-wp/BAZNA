package com.brendigo.sidrena.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.SiteUrl

internal const val BAZNA_PRIVACY_URL = "https://brendigo.com/politika-privatnosti/"

/** Open a link only after the existing same-origin HTTPS validation. */
internal fun openBaznaPublicLink(context: Context, snapshot: CatalogSnapshot?, url: String) {
    val verified = runCatching { snapshot?.let { SiteUrl.parse(it.site).publicLink(url) } }
        .getOrNull()
    if (verified == null) {
        Toast.makeText(context, "Poveznica nije dostupna.", Toast.LENGTH_SHORT).show()
        return
    }
    openExternalIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(verified))
        .addCategory(Intent.CATEGORY_BROWSABLE))
}

internal fun openBaznaPrivacy(context: Context) {
    openExternalIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(BAZNA_PRIVACY_URL))
        .addCategory(Intent.CATEGORY_BROWSABLE))
}

internal fun openBaznaSupport(context: Context) {
    openExternalIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@brendigo.com")))
}

/** External browser/mail apps may be missing or restricted by device policy. */
private fun openExternalIntent(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nema aplikacije za otvaranje poveznice.", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, "Otvaranje poveznice nije dopušteno.", Toast.LENGTH_SHORT).show()
    }
}
