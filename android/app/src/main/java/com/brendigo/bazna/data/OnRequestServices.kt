package com.brendigo.bazna.data

import org.json.JSONArray
import org.json.JSONObject

/** Shared published-only inquiry extension for WordPress, PHP, Joomla and Shopify. */
object OnRequestServices {
    private const val MAX_ITEMS = 500
    private val idPattern = Regex("[A-Za-z0-9-]{1,64}")

    fun parse(json: JSONObject, outlets: List<Outlet>): List<OnRequestService> {
        require(json.optInt("schema", -1) == 1) {
            "Nepodržan format usluga na upit."
        }
        val source = json.opt("items")
        require(source is JSONArray && source.length() <= MAX_ITEMS) {
            "Neispravan javni popis usluga na upit."
        }
        val validOutlets = outlets.map { it.code }.toSet()
        val seen = HashSet<String>()
        return buildList {
            for (i in 0 until source.length()) {
                val row = source.optJSONObject(i)
                    ?: throw IllegalArgumentException("Neispravan redak usluge na upit.")
                fun text(field: String, max: Int): String {
                    val value = row.opt(field)
                    require(value is String && value.length <= max &&
                        value.none(Char::isISOControl)) {
                        "Neispravno polje usluge na upit."
                    }
                    return value.trim()
                }
                val id = text("id", 64)
                val name = text("name", 240)
                val criteria = text("criteria", 800)
                val outlet = text("outlet", 64)
                require(idPattern.matches(id) && seen.add(id) &&
                    name.isNotBlank() && criteria.isNotBlank() &&
                    (outlet.isBlank() || outlet in validOutlets)) {
                    "Duplikat ili nevaljana usluga na upit."
                }
                // Inquiry-only records never carry a guessed numeric amount.
                require(!row.has("price") && !row.has("maloprodajna_cijena")
                    && !row.has("sidrena_cijena")) {
                    "Usluga na upit ne smije glumiti numerički cjenik."
                }
                add(OnRequestService(id, name, criteria, outlet))
            }
        }
    }

    fun visible(
        records: List<OnRequestService>, outlet: String, query: String,
        priceFilter: Int,
    ): List<OnRequestService> {
        if (priceFilter != 0) return emptyList()
        val search = query.trim().take(256)
        return records.filter {
            it.outlet == outlet && (search.isEmpty() ||
                it.name.contains(search, ignoreCase = true) ||
                it.criteria.contains(search, ignoreCase = true))
        }
    }
}
