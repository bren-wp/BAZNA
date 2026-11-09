package com.brendigo.sidrena.data

data class Outlet(
    val name: String,
    val code: String,
    val productCsv: String,
    val serviceCsv: String,
    val productsCsvReady: Boolean? = null,
    val servicesCsvReady: Boolean? = null,
    val address: String = "",
)

data class CatalogIndex(
    val publishedAt: String?,
    val publicPage: String?,
    val archivePage: String?,
    val productsCsv: String,
    val servicesCsv: String,
    val outlets: List<Outlet>,
    val pluginVersion: String? = null,
    val legal: LegalContract? = null,
    val productsCsvReady: Boolean? = null,
    val servicesCsvReady: Boolean? = null,
    val pendingPublication: Boolean? = null,
    val wordpressLicenseActive: Boolean? = null,
    val platform: String = "wordpress",
)

data class PriceItem(
    val id: String,
    val name: String,
    val kind: String,
    val currentPrice: String,
    val referencePrice: String,
    val referenceDate: String,
    val sku: String,
    val availability: String,
    val unit: String,
    val imageUrl: String = "",
    val unitPrice: String = "",
    val saleLabel: String = "",
    val barcode: String = "",
    val brand: String = "",
)

data class ArchiveEntry(
    val date: String,
    val kind: String,
    val outlet: String,
    val format: String,
    val url: String,
)

data class CatalogSnapshot(
    val site: String,
    val index: CatalogIndex,
    val products: List<PriceItem>,
    val services: List<PriceItem>,
    val archive: List<ArchiveEntry>,
    val warnings: List<String>,
)
