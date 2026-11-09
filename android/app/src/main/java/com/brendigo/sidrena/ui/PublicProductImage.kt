package com.brendigo.sidrena.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.brendigo.sidrena.data.PublicNetworkGuard
import com.brendigo.sidrena.data.SiteUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Optional photo from the public BAZNA product CSV. Only a same-origin HTTPS
 * image is permitted; missing or unsupported sources use the local pictogram.
 */
internal fun approvedProductImageUrl(site: String, image: String): String? =
    if (image.isBlank()) null
    else runCatching { SiteUrl.parse(site).publicLink(image) }.getOrNull()

internal object PublicProductImages {
    private const val MAX_BYTES = 1_500_000
    private const val MAX_PIXELS = 16_000_000L
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val http = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .proxy(Proxy.NO_PROXY)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                require(addresses.isNotEmpty() && addresses.all(PublicNetworkGuard::isPublic)) {
                    "Slike se učitavaju samo s javnih mrežnih adresa."
                }
                return addresses
            }
        })
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun get(url: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(url)?.let { return@withContext it }
        try {
            val request = Request.Builder().url(url).get()
                .header("Accept", "image/jpeg,image/png,image/webp")
                .header("User-Agent", "BAZNA-Android")
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val media = response.header("Content-Type").orEmpty()
                    .substringBefore(';').trim().lowercase()
                if (media !in setOf("image/jpeg", "image/png", "image/webp")) {
                    return@withContext null
                }
                val body = response.body ?: return@withContext null
                if (body.contentLength() > MAX_BYTES) return@withContext null
                val output = ByteArrayOutputStream()
                body.byteStream().use { input ->
                    val chunk = ByteArray(8192)
                    while (true) {
                        val n = input.read(chunk)
                        if (n < 0) break
                        if (output.size() + n > MAX_BYTES) return@withContext null
                        output.write(chunk, 0, n)
                    }
                }
                val bytes = output.toByteArray()
                val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, dimensions)
                val width = dimensions.outWidth
                val height = dimensions.outHeight
                if (width < 1 || height < 1 || width > 8192 || height > 8192 ||
                    width.toLong() * height.toLong() > MAX_PIXELS
                ) return@withContext null
                val options = BitmapFactory.Options().apply {
                    inSampleSize = maxOf(1, maxOf(width / 240, height / 280))
                }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    ?: return@withContext null
                cache.put(url, decoded)
                decoded
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
internal fun ProductThumbnail(
    site: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    val approved = remember(site, imageUrl) { approvedProductImageUrl(site, imageUrl) }
    var image by remember(approved) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(approved) {
        if (approved != null) image = PublicProductImages.get(approved)
    }
    Box(
        modifier = modifier
            .background(Color(0xFFF0EEE8), RoundedCornerShape(9.dp))
            .clip(RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = image
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(
                Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = Color(0xFF996F4D),
                modifier = Modifier.size(35.dp),
            )
        }
    }
}
