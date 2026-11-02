package com.brendigo.sidrena.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brendigo.sidrena.data.ArchiveEntry
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.Outlet
import com.brendigo.sidrena.data.PriceItem
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real emulator smoke + screenshot capture, not a claim of pixel-diff parity.
 * CI sets each physical viewport/density before running this same test.
 * Test-only public data never leaves this instrumented process.
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveScreensTest {
    @get:Rule val compose = createComposeRule()

    private val product = PriceItem(
        id = "test-product", name = "Testni proizvod", kind = "proizvod",
        currentPrice = "5,00", referencePrice = "6,00",
        referenceDate = "10. 9. 2026.", sku = "TEST",
        availability = "Dostupno", unit = "kom",
    )
    private val service = product.copy(
        id = "test-service", name = "Testna usluga",
        kind = "usluga", sku = "SERVICE",
    )
    private val snapshot = CatalogSnapshot(
        site = "https://example.org",
        index = CatalogIndex(
            publishedAt = "2026-11-02",
            publicPage = "https://example.org/cjenici",
            archivePage = "https://example.org/arhiva",
            productsCsv = "https://example.org/proizvodi.csv",
            servicesCsv = "https://example.org/usluge.csv",
            outlets = listOf(
                Outlet("Testna poslovnica", "test", "", ""),
            ),
        ),
        products = listOf(product),
        services = listOf(service),
        archive = listOf(
            ArchiveEntry("2026-10-31", "proizvodi", "webshop", "csv", "https://example.org/archive.csv"),
        ),
        warnings = emptyList(),
    )

    @Test
    fun captureTwelvePublicScreensForViewport() {
        val noop = {}
        val names = listOf(
            "01-splash", "02-connection", "03-home", "04-products",
            "05-services", "06-detail", "07-cjenici", "08-archive",
            "09-locations", "10-search", "11-settings", "12-about",
        )
        var screen by mutableIntStateOf(0)
        compose.setContent {
            SidrenaTheme {
                if (screen <= 1) {
                    Box(Modifier.fillMaxSize()) {
                        when (screen) {
                            0 -> SidrenaSplashArtwork()
                            else -> ConnectionPage(
                                input = "", busy = false, error = null, canCancel = false,
                                onInput = {}, onConnect = {}, onCancel = noop,
                            )
                        }
                    }
                } else {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0),
                        bottomBar = { AppNavigation(active = 0, onSelect = {}) },
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                            when (screen) {
                                2 -> HomePage(snapshot, false, null, {}, noop, noop, {})
                                3 -> ProductCatalogScreen(snapshot, false, null, {}, noop, noop, noop)
                                4 -> ServiceCatalogScreen(snapshot, false, null, {}, noop, noop, noop)
                                5 -> DetailPage(product, snapshot, noop, {})
                                6 -> PublicPriceListsScreen(snapshot, "", false, null, noop, {}, noop, noop)
                                7 -> ArchivePage(snapshot, false, null, {}, noop, noop, noop)
                                8 -> LocationsPage(snapshot, "", false, null, noop, {}, noop, noop)
                                9 -> SearchPage(snapshot, false, null, "", 0, {}, {}, {}, noop, noop, noop)
                                10 -> SettingsPage(snapshot, "", false, null, noop, noop, noop, noop, noop, noop, noop)
                                11 -> AboutPage(snapshot, noop, noop, noop, noop, {})
                            }
                        }
                    }
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "responsive")
        assertTrue(output.exists() || output.mkdirs())

        names.forEachIndexed { index, name ->
            compose.runOnIdle { screen = index }
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            assertTrue("Empty render for $name", bitmap.width > 0 && bitmap.height > 0)
            File(output, "$name.png").outputStream().use {
                assertTrue("PNG encode failed for $name", bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        }
    }
}
