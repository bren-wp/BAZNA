package com.brendigo.bazna.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brendigo.bazna.SidrenaViewModel
import com.brendigo.bazna.data.CatalogSnapshot

@Composable
fun SidrenaApp(model: SidrenaViewModel = viewModel()) {
    val state = model.state
    val snapshot = state.snapshot
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf("home") }
    var selectedItemFields by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var selectedItemOutlet by rememberSaveable { mutableStateOf("") }
    var selectedItemSite by rememberSaveable { mutableStateOf("") }
    var detailReturnPage by rememberSaveable { mutableStateOf("products") }
    var searchReturnPage by rememberSaveable { mutableStateOf("home") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchKind by rememberSaveable { mutableStateOf(0) }
    var alert by remember { mutableStateOf<String?>(null) }
    var askDisconnect by remember { mutableStateOf(false) }

    fun navigate(destination: String) {
        if (shouldRecordSearchReturn(page, destination)) {
            searchReturnPage = page
        }
        page = destination
        if (destination != "detail") {
            selectedItemFields = emptyList()
            selectedItemOutlet = ""
            selectedItemSite = ""
        }
    }

    fun openPublic(url: String) = openBaznaPublicLink(context, snapshot, url)

    fun supportEmail() = openBaznaSupport(context)

    BackHandler(enabled = page != "home" && !state.isEditingSite) {
        navigate(backDestination(page, detailReturnPage, searchReturnPage))
    }

    if (askDisconnect) {
        AlertDialog(
            onDismissRequest = { askDisconnect = false },
            title = { Text("Odspojiti stranicu?") },
            text = { Text("Adresa će se ukloniti iz aplikacije. Podaci na povezanoj BAZNA stranici neće se mijenjati.") },
            confirmButton = {
                TextButton(onClick = {
                    model.disconnect()
                    askDisconnect = false
                    navigate("home")
                }) { Text("Odspoji") }
            },
            dismissButton = {
                TextButton(onClick = { askDisconnect = false }) { Text("Odustani") }
            },
        )
    }
    alert?.let { message ->
        AlertDialog(
            onDismissRequest = { alert = null },
            title = { Text("BAZNA") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { alert = null }) { Text("U redu") } },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Background).systemBarsPadding(),
        contentWindowInsets = WindowInsets(0),
        containerColor = Background,
        bottomBar = {
            if (snapshot != null && !state.isEditingSite) {
                AppNavigation(
                    active = bottomTabForPage(page, searchReturnPage),
                    onSelect = { index -> navigate(pageForBottomTab(index)) },
                )
            }
        },
    ) { insets ->
        // Preserve the original phone layout, but stop stretching a single
        // column and its price cards edge-to-edge on tablets/landscape screens.
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentAlignment = Alignment.TopCenter,
        ) {
            val connecting = state.isEditingSite || snapshot == null
            val maxDp = if (connecting) AdaptiveSizing.MAX_CONNECTION_WIDTH_DP
                else AdaptiveSizing.MAX_CONTENT_WIDTH_DP
            Column(
                Modifier.width(minOf(maxWidth, maxDp.dp)).fillMaxHeight()
                    .padding(horizontal = if (connecting) 0.dp else 16.dp)
            ) {
            if (state.isEditingSite || snapshot == null) {
                ConnectionPage(
                    input = state.siteInput,
                    busy = state.isLoading,
                    error = state.error,
                    canCancel = snapshot != null,
                    onInput = model::setSiteInput,
                    onConnect = { value ->
                        navigate("home")
                        model.connect(value)
                    },
                    onCancel = {
                        if (snapshot != null) model.cancelEdit()
                        else (context as? Activity)?.finish()
                    },
                )
            } else {
                // Home screen has its own wordmark/search header from the reference.
                // Keep the shared header unchanged on all other screens.
                if (page !in setOf("home", "products", "services", "detail", "cjenici", "archive", "locations", "search", "settings", "about")) {
                    BrandHeader(onNotifications = {
                        alert = "Posljednja objava: " +
                            (snapshot.index.publishedAt ?: "Datum nije dostupan")
                    })
                }
                when (page) {
                    "home" -> HomePage(
                        data = snapshot,
                        busy = state.isLoading,
                        error = state.error,
                        onNavigate = ::navigate,
                        onRefresh = model::refresh,
                        onEdit = model::editSite,
                        onOpen = ::openPublic,
                        selectedOutlet = state.outlet,
                    )
                    "products" -> ProductCatalogScreen(
                        data = snapshot,
                        busy = state.isLoading,
                        error = state.error,
                        onDetails = {
                            selectedItemFields = saveDetailItem(it)
                            selectedItemOutlet = state.outlet
                            selectedItemSite = snapshot.site
                            detailReturnPage = "products"
                            navigate("detail")
                        },
                        onBack = { navigate("home") },
                        onShowServices = { navigate("services") },
                        onRefresh = model::refresh,
                    )
                    "services" -> ServiceCatalogScreen(
                        data = snapshot,
                        busy = state.isLoading,
                        error = state.error,
                        selectedOutlet = state.outlet,
                        onDetails = {
                            selectedItemFields = saveDetailItem(it)
                            selectedItemOutlet = state.outlet
                            selectedItemSite = snapshot.site
                            detailReturnPage = "services"
                            navigate("detail")
                        },
                        onBack = { navigate("home") },
                        onShowProducts = { navigate("products") },
                        onRefresh = model::refresh,
                    )
                    "cjenici" -> PublicPriceListsScreen(
                        data = snapshot,
                        selectedOutlet = state.outlet,
                        busy = state.isLoading,
                        error = state.error,
                        onBack = { navigate("home") },
                        onOpen = ::openPublic,
                        onArchive = { navigate("archive") },
                        onRefresh = model::refresh,
                    )
                    "archive" -> ArchivePage(
                        data = snapshot,
                        busy = state.isLoading,
                        error = state.error,
                        onOpen = ::openPublic,
                        onBack = { navigate("home") },
                        onPriceLists = { navigate("cjenici") },
                        onRefresh = model::refresh,
                    )
                    "locations" -> LocationsPage(
                        data = snapshot,
                        selectedOutlet = state.outlet,
                        busy = state.isLoading,
                        error = state.error,
                        onBack = { navigate("home") },
                        onSelect = model::selectOutlet,
                        onPriceLists = { navigate("cjenici") },
                        onRefresh = model::refresh,
                    )
                    "search" -> SearchPage(
                        data = snapshot,
                        busy = state.isLoading,
                        error = state.error,
                        query = searchQuery,
                        kind = searchKind,
                        onQueryChange = { searchQuery = it.take(256) },
                        onKindChange = { searchKind = it },
                        onDetails = {
                            selectedItemFields = saveDetailItem(it)
                            selectedItemOutlet = state.outlet
                            selectedItemSite = snapshot.site
                            detailReturnPage = "search"
                            navigate("detail")
                        },
                        onBack = { navigate(searchReturnPage) },
                        onArchive = { navigate("archive") },
                        onRefresh = model::refresh,
                    )
                    "detail" -> DetailPage(
                        item = resolveCurrentDetailItem(
                            saved = restoreDetailItem(selectedItemFields),
                            snapshot = snapshot,
                            savedOutlet = selectedItemOutlet,
                            currentOutlet = state.outlet,
                            savedSite = selectedItemSite,
                        ),
                        data = snapshot,
                        onBack = { navigate(detailReturnPage) },
                        onOpen = ::openPublic,
                    )
                    "settings" -> SettingsPage(
                        data = snapshot,
                        selectedOutlet = state.outlet,
                        busy = state.isLoading,
                        error = state.error,
                        onBack = { navigate("home") },
                        onEditSite = model::editSite,
                        onRefresh = model::refresh,
                        onDisconnect = { askDisconnect = true },
                        onOutlets = { navigate("locations") },
                        onAbout = { navigate("about") },
                        onHelp = { navigate("about") },
                    )
                    "about" -> AboutPage(
                        data = snapshot,
                        onBack = { navigate("settings") },
                        onConnectHelp = { alert = "Unesite HTTPS adresu stranice koja objavljuje BAZNA cjenike (WordPress, Web PHP, Joomla ili Shopify). Nakon povezivanja odaberite poslovnicu i pregledajte objavljene podatke." },
                        onAnchorHelp = { alert = "Bazna cijena je dokumentirana referentna vrijednost uz datum. Uvijek je provjerite na izvornoj povezanoj BAZNA stranici." },
                        onSupport = ::supportEmail,
                        onPrivacy = { openBaznaPrivacy(context) },
                        onOpenPublic = ::openPublic,
                    )
                    else -> HomePage(
                        data = snapshot,
                        busy = state.isLoading,
                        error = state.error,
                        onNavigate = ::navigate,
                        onRefresh = model::refresh,
                        onEdit = model::editSite,
                        onOpen = ::openPublic,
                        selectedOutlet = state.outlet,
                    )
                }
            }
            }
        }
    }
}
