package io.github.dospe.exifcsv.ui

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_SIZE = 320

/** Small in-memory cache so scrolling back does not decode thumbnails again. */
private object ThumbnailCache {
    private val maxBytes = (Runtime.getRuntime().maxMemory() / 8).toInt().coerceAtLeast(8 * 1024 * 1024)
    private val cache = object : LruCache<String, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun get(uri: Uri): ImageBitmap? = cache.get(uri.toString())
    fun put(uri: Uri, bitmap: ImageBitmap) {
        cache.put(uri.toString(), bitmap)
    }
}

@Suppress("DEPRECATION")
private fun loadThumbnail(context: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(uri, Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null)
    } else {
        MediaStore.Images.Thumbnails.getThumbnail(
            context.contentResolver,
            ContentUris.parseId(uri),
            MediaStore.Images.Thumbnails.MINI_KIND,
            null,
        )
    }
}.getOrNull()

@Composable
fun PhotoThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(initialValue = ThumbnailCache.get(uri), key1 = uri) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                loadThumbnail(context, uri)?.asImageBitmap()?.also { ThumbnailCache.put(uri, it) }
            }
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
