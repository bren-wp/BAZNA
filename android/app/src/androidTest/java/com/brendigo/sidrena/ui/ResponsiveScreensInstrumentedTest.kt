package com.brendigo.sidrena.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brendigo.sidrena.data.ArchiveEntry
import com.brendigo.sidrena.data.CatalogIndex
import com.brendigo.sidrena.data.CatalogSnapshot
import com.brendigo.sidrena.data.Outlet
import com.brendigo.sidrena.data.PriceItem
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs 12 actual Compose screens in an emulator at the device dimensions set
 * by tools/run_android_responsive.sh; captures every resulting frame.
 *
 * All sample catalog values exist ONLY in this androidTest source. The shipped
 * application reads real public WordPress data, never these fixtures.
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveScreensInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val product = PriceItem(
        id = "p1", name = "Maslinovo ulje 1 L", kind = "proizvod",
        currentPrice = "5,99", referencePrice = "8,49",
        referenceDate = "10.09.2026.", sku = "MU-001",
        availability = "dostupno", unit = "1 L",
        unitPrice = "5,99", brand = "Domaće",
    )
    private val service = PriceItem(
        id = "s1", name = "Servis uređaja", kind = "usluga",
        currentPrice = "45,00", referencePrice = "60,00",
        referenceDate = "10.09.2026.", sku = "SR-001",
        availability = "dostupno", unit = "sat",
    )
    private val snapshot = CatalogSnapshot(
        site = "https://primjer.hr",
        index = CatalogIndex(
            publishedAt = "2026-11-02T08:00:00Z",
            publicPage = "https://primjer.hr/cjenici",
            archivePage = "https://primjer.hr/arhiva",
            productsCsv = "https://primjer.hr/proizvodi.csv",
            servicesCsv = "https://primjer.hr/usluge.csv",
            outlets = listOf(
                Outlet(
                    name = "Rijeka", code = "rijeka",
                    productCsv = "https://primjer.hr/rijeka-proizvodi.csv",
                    serviceCsv = "https://primjer.hr/rijeka-usluge.csv",
                    productsCsvReady = true, servicesCsvReady = true,
                ),
            ),
            productsCsvReady = true,
            servicesCsvReady = true,
        ),
        products = listOf(product),
        services = listOf(service),
        archive = listOf(
            ArchiveEntry(
                date = "2026-10-31", kind = "proizvodi", outlet = "webshop",
                format = "csv", url = "https://primjer.hr/2026-10-31.csv",
            ),
        ),
        warnings = emptyList(),
    )

    @Test fun twelveScreensAreRenderableAndTappableOnEveryConfiguredDisplay() {
        val currentScreen = mutableIntStateOf(0)
        compose.setContent {
            SidrenaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                        .background(Background).systemBarsPadding(),
                    containerColor = Background,
                    contentWindowInsets = WindowInsets(0),
                    bottomBar = {
                        if (currentScreen.intValue >= 2) {
                            AppNavigation(
                                active = when (currentScreen.intValue) {
                                    0, 1, 2, 8 -> 0
                                    7 -> 2
                                    10, 11 -> 3
                                    else -> 1
                                },
                                onSelect = {},
                            )
                        }
                    },
                ) { insets ->
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize().padding(insets),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        val connecting = currentScreen.intValue < 2
                        val maxDp = if (connecting)
                            AdaptiveSizing.MAX_CONNECTION_WIDTH_DP
                        else AdaptiveSizing.MAX_CONTENT_WIDTH_DP
                        Column(
                            Modifier.width(minOf(maxWidth, maxDp.dp)).fillMaxHeight()
                                .padding(horizontal = if (connecting) 0.dp else 16.dp),
                        ) {
                            when (currentScreen.intValue) {
                                0 -> SidrenaSplashScreen(onFinished = {})
                                1 -> ConnectionPage(
                                    input = "https://primjer.hr", busy = false,
                                    error = null, canCancel = false,
                                    onInput = {}, onConnect = {}, onCancel = {},
                                )
                                2 -> HomePage(
                                    data = snapshot, busy = false, error = null,
                                    onNavigate = {}, onRefresh = {}, onEdit = {},
                                    onOpen = {},
                                )
                                3 -> ProductCatalogScreen(
                                    data = snapshot, busy = false, error = null,
                                    onDetails = {}, onBack = {}, onShowServices = {},
                                    onRefresh = {},
                                )
                                4 -> ServiceCatalogScreen(
                                    data = snapshot, busy = false, error = null,
                                    onDetails = {}, onBack = {}, onShowProducts = {},
                                    onRefresh = {},
                                )
                                5 -> DetailPage(
                                    item = product, data = snapshot,
                                    onBack = {}, onOpen = {},
                                )
                                6 -> PublicPriceListsScreen(
                                    data = snapshot, selectedOutlet = "",
                                    busy = false, error = null,
                                    onBack = {}, onOpen = {}, onArchive = {},
                                    onRefresh = {},
                                )
                                7 -> ArchivePage(
                                    data = snapshot, busy = false, error = null,
                                    onOpen = {}, onBack = {}, onPriceLists = {},
                                    onRefresh = {},
                                )
                                8 -> LocationsPage(
                                    data = snapshot, selectedOutlet = "",
                                    busy = false, error = null,
                                    onBack = {}, onSelect = {},
                                    onPriceLists = {}, onRefresh = {},
                                )
                                9 -> SearchPage(
                                    data = snapshot, busy = false, error = null,
                                    query = "", kind = 0, onQueryChange = {},
                                    onKindChange = {}, onDetails = {},
                                    onBack = {}, onArchive = {}, onRefresh = {},
                                )
                                10 -> SettingsPage(
                                    data = snapshot, selectedOutlet = "",
                                    busy = false, error = null,
                                    onBack = {}, onEditSite = {}, onRefresh = {},
                                    onDisconnect = {}, onOutlets = {},
                                    onAbout = {}, onHelp = {},
                                )
                                11 -> AboutPage(
                                    data = snapshot, onBack = {},
                                    onConnectHelp = {}, onAnchorHelp = {},
                                    onSupport = {}, onOpenPublic = {},
                                )
                            }
                        }
                    }
                }
            }
        }

        val names = listOf(
            "01-splash", "02-connect", "03-home", "04-products",
            "05-services", "06-detail", "07-documents", "08-archive",
            "09-outlets", "10-search", "11-settings", "12-about",
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(
            instrumentation.targetContext.getExternalFilesDir(null), "responsive"
        )
        assertTrue("Cannot create screenshot directory", directory.isDirectory || directory.mkdirs())

        names.forEachIndexed { index, name ->
            compose.runOnIdle { currentScreen.intValue = index }
            compose.waitForIdle()

            val root = compose.onRoot().fetchSemanticsNode()
            val bounds = root.boundsInRoot
            assertTrue("Screen has no width at " + name, bounds.width > 0f)
            assertTrue("Screen has no height at " + name, bounds.height > 0f)

            // Assert tappable controls don't run off the left/right display edge.
            // Vertically offscreen content remains reachable through the scroll
            // containers and must not be treated as a failure.
            val buttons = compose.onAllNodes(hasClickAction(), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
            buttons.forEach { button ->
                val rect: Rect = button.boundsInRoot
                if (rect.width > 0f && rect.height > 0f &&
                    rect.bottom > bounds.top && rect.top < bounds.bottom
                ) {
                    assertTrue(
                        name + ": tap target extends past left edge: " + rect,
                        rect.left >= bounds.left - 2f,
                    )
                    assertTrue(
                        name + ": tap target extends past right edge: " + rect,
                        rect.right <= bounds.right + 2f,
                    )
                }
            }
            val capture = instrumentation.uiAutomation.takeScreenshot()
            assertTrue("Could not capture " + name, capture != null)
            FileOutputStream(File(directory, name + ".png")).use { output ->
                capture!!.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            capture?.recycle()
        }
    }
}
