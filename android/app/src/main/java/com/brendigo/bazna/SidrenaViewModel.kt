package com.brendigo.bazna

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brendigo.bazna.data.CatalogRepository
import com.brendigo.bazna.data.CatalogSnapshot
import com.brendigo.bazna.data.SiteUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class SidrenaState(
    val siteInput: String = "",
    val snapshot: CatalogSnapshot? = null,
    val outlet: String = "",
    val isLoading: Boolean = false,
    val isEditingSite: Boolean = true,
    val error: String? = null,
)

class SidrenaViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("sidrena_public_site", 0)
    private val repository = CatalogRepository()
    var state by mutableStateOf(SidrenaState())
        private set
    private var requestId = 0L

    init {
        val saved = preferences.getString("site", "").orEmpty()
        if (saved.isNotBlank()) {
            val savedOutlet = preferences.getString("outlet", "").orEmpty()
            load(saved, savedOutlet, newSite = true, restoringSession = true)
        }
    }

    fun setSiteInput(value: String) {
        state = state.copy(siteInput = value, error = null)
    }

    fun editSite() {
        requestId++
        state = state.copy(isEditingSite = true, isLoading = false, error = null)
    }

    fun cancelEdit() {
        requestId++
        val snapshot = state.snapshot ?: return
        state = state.copy(isEditingSite = false, isLoading = false,
            siteInput = snapshot.site, error = null)
    }

    fun disconnect() {
        requestId++
        preferences.edit().remove("site").remove("outlet").apply()
        state = SidrenaState()
    }

    fun connect(raw: String = state.siteInput) {
        val site = try { SiteUrl.parse(raw).display } catch (exception: IllegalArgumentException) {
            state = state.copy(siteInput = raw, isEditingSite = true, error = exception.message)
            return
        }
        load(site, "", true)
    }

    fun selectOutlet(code: String) {
        val site = state.snapshot?.site ?: return
        if (code == state.outlet) return
        load(site, code, false)
    }

    fun refresh() {
        val site = state.snapshot?.site ?: return
        load(site, state.outlet, false)
    }

    private fun load(
        site: String,
        outlet: String,
        newSite: Boolean,
        restoringSession: Boolean = false,
    ) {
        val id = ++requestId
        state = state.copy(siteInput = site, isLoading = true, error = null,
            isEditingSite = if (newSite) true else state.isEditingSite)
        viewModelScope.launch {
            try {
                // Reconnect using the saved outlet, not the default webshop.
                // A deleted outlet is the only case where a fallback is safe.
                val loaded = try {
                    repository.load(site, outlet)
                } catch (missing: IllegalArgumentException) {
                    if (!restoringSession || outlet.isBlank() ||
                        missing.message != "Odabrana poslovnica više ne postoji."
                    ) throw missing
                    repository.load(site, "")
                }
                if (id != requestId) return@launch
                val effectiveOutlet = if (outlet.isNotBlank() &&
                    loaded.index.outlets.none { it.code == outlet }
                ) "" else outlet
                preferences.edit()
                    .putString("site", loaded.site)
                    .putString("outlet", effectiveOutlet)
                    .apply()
                state = state.copy(siteInput = loaded.site, snapshot = loaded,
                    outlet = effectiveOutlet, isEditingSite = false, isLoading = false, error = null)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (id != requestId) return@launch
                state = state.copy(isLoading = false,
                    error = "Povezivanje nije uspjelo. Provjerite adresu, mrežu i objavljene cjenike.")
            }
        }
    }
}
