package io.github.dospe.exifcsv.exif

/** Converts EXIF timestamps ("2026:10:06 14:32:10") to "2026-10-06 14:32:10". Pure Kotlin. */
object ExifDateParser {
    private val EXIF_PATTERN = Regex("""^(\d{4}):(\d{2}):(\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?$""")
    private val ISO_PATTERN =
        Regex("""^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?$""")

    /** Returns the normalised timestamp, the trimmed raw value if it is in an unknown format, or null when empty. */
    fun normalize(raw: String?): String? {
        if (raw == null) return null
        val value = raw.trim().trimEnd('\u0000').trim()
        if (value.isEmpty()) return null
        // Placeholders some cameras write: "0000:00:00 00:00:00" or "    :  :     :  :  ".
        if (value.all { it == ':' || it == ' ' || it == '0' }) return null
        EXIF_PATTERN.matchEntire(value)?.let { m ->
            val (y, mo, d, h, mi, s) = m.destructured
            return "$y-$mo-$d $h:$mi:$s"
        }
        ISO_PATTERN.matchEntire(value)?.let { m ->
            val (y, mo, d, h, mi, s) = m.destructured
            return "$y-$mo-$d $h:$mi:$s"
        }
        return value
    }

    /** Picks the best available timestamp: DateTimeOriginal, then DateTimeDigitized, then DateTime. */
    fun pick(original: String?, digitized: String?, modified: String?): String? =
        normalize(original) ?: normalize(digitized) ?: normalize(modified)
}
