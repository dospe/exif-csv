package io.github.dospe.exifcsv.data

import android.net.Uri

data class PhotoItem(
    val uri: Uri,
    val name: String,
    val dateTakenMillis: Long?,
)
