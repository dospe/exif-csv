package io.github.dospe.exifcsv.exif

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import io.github.dospe.exifcsv.csv.PhotoRecord

/**
 * Reads capture time and GPS position from a photo.
 *
 * Since Android 10 the system strips GPS tags from photos served through MediaStore unless the app
 * holds ACCESS_MEDIA_LOCATION and asks for the original bytes via [MediaStore.setRequireOriginal].
 * Documents picked with the system file picker usually keep their tags; for the media documents
 * provider we additionally map the document to its MediaStore URI and try the original there.
 */
class ExifReader(private val context: Context) {

    private val resolver: ContentResolver get() = context.contentResolver

    fun read(uri: Uri, name: String): PhotoRecord {
        var fallback: PhotoRecord? = null
        for (candidate in candidates(uri)) {
            val record = readFrom(candidate, name) ?: continue
            if (record.hasGps) return record
            if (fallback == null) fallback = record
        }
        return fallback ?: PhotoRecord(name, null, null, null, null)
    }

    /** URIs to try, most promising first. */
    private fun candidates(uri: Uri): List<Uri> {
        val result = ArrayList<Uri>(2)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mediaUri: Uri? = when (uri.authority) {
                MediaStore.AUTHORITY -> uri
                MEDIA_DOCUMENTS_AUTHORITY -> runCatching { MediaStore.getMediaUri(context, uri) }.getOrNull()
                else -> null
            }
            if (mediaUri != null) result.add(MediaStore.setRequireOriginal(mediaUri))
        }
        result.add(uri)
        return result
    }

    private fun readFrom(uri: Uri, name: String): PhotoRecord? {
        val exif = runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { pfd -> ExifInterface(pfd.fileDescriptor) }
        }.getOrNull() ?: return null

        val latLong: DoubleArray? = exif.latLong
        val altitude = exif.getAltitude(Double.NaN).takeUnless { it.isNaN() }
        val dateTime = ExifDateParser.pick(
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
            exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED),
            exif.getAttribute(ExifInterface.TAG_DATETIME),
        )
        return PhotoRecord(
            name = name,
            dateTime = dateTime,
            latitude = latLong?.get(0),
            longitude = latLong?.get(1),
            altitude = altitude,
        )
    }

    private companion object {
        const val MEDIA_DOCUMENTS_AUTHORITY = "com.android.providers.media.documents"
    }
}
