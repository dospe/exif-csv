package io.github.dospe.exifcsv.csv

import java.util.Locale

data class CsvOptions(
    val delimiter: Char = ';',
    /** Write 50,087500 instead of 50.087500 (what Excel in Czech locale expects). */
    val decimalComma: Boolean = false,
    /** UTF-8 byte order mark so Excel recognises the encoding. */
    val includeBom: Boolean = true,
    val lineSeparator: String = "\r\n",
)

object CsvBuilder {
    const val BOM = "﻿"
    val HEADER: List<String> = listOf("nazev", "datum_cas", "lat", "lon", "alt")

    fun build(records: List<PhotoRecord>, options: CsvOptions = CsvOptions()): String {
        val delimiter = options.delimiter.toString()
        val sb = StringBuilder()
        if (options.includeBom) sb.append(BOM)
        sb.append(HEADER.joinToString(delimiter) { escape(it, options.delimiter) })
        sb.append(options.lineSeparator)
        for (record in records) {
            val cells = listOf(
                record.name,
                record.dateTime ?: "",
                formatNumber(record.latitude, 6, options.decimalComma),
                formatNumber(record.longitude, 6, options.decimalComma),
                formatNumber(record.altitude, 1, options.decimalComma),
            )
            sb.append(cells.joinToString(delimiter) { escape(it, options.delimiter) })
            sb.append(options.lineSeparator)
        }
        return sb.toString()
    }

    fun formatNumber(value: Double?, decimals: Int, decimalComma: Boolean): String {
        if (value == null || value.isNaN() || value.isInfinite()) return ""
        val text = String.format(Locale.ROOT, "%.${decimals}f", value)
        return if (decimalComma) text.replace('.', ',') else text
    }

    /** RFC 4180 quoting: wrap in quotes when the value contains the delimiter, a quote or a line break. */
    fun escape(value: String, delimiter: Char): String {
        val needsQuotes = value.indexOf(delimiter) >= 0 ||
            value.indexOf('"') >= 0 ||
            value.indexOf('\n') >= 0 ||
            value.indexOf('\r') >= 0
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
