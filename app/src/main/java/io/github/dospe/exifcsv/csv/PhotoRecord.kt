package io.github.dospe.exifcsv.csv

/** One row of the resulting CSV. Pure Kotlin so it can be unit-tested on the JVM. */
data class PhotoRecord(
    val name: String,
    /** Normalised capture time, "yyyy-MM-dd HH:mm:ss", or null when the photo has none. */
    val dateTime: String?,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
) {
    val hasGps: Boolean get() = latitude != null && longitude != null
}
