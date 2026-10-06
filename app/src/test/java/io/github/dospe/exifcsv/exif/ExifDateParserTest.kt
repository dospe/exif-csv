package io.github.dospe.exifcsv.exif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExifDateParserTest {

    @Test
    fun `exif format is converted to iso like format`() {
        assertEquals("2026-10-06 14:32:10", ExifDateParser.normalize("2026:10:06 14:32:10"))
    }

    @Test
    fun `fractional seconds are dropped`() {
        assertEquals("2026-10-06 14:32:10", ExifDateParser.normalize("2026:10:06 14:32:10.123"))
    }

    @Test
    fun `iso timestamps with offset are accepted`() {
        assertEquals("2026-10-06 14:32:10", ExifDateParser.normalize("2026-10-06T14:32:10+02:00"))
        assertEquals("2026-10-06 14:32:10", ExifDateParser.normalize("2026-10-06T14:32:10Z"))
    }

    @Test
    fun `blank and placeholder values become null`() {
        assertNull(ExifDateParser.normalize(null))
        assertNull(ExifDateParser.normalize(""))
        assertNull(ExifDateParser.normalize("   "))
        assertNull(ExifDateParser.normalize("0000:00:00 00:00:00"))
        assertNull(ExifDateParser.normalize("    :  :     :  :  "))
        assertNull(ExifDateParser.normalize("2026:10:06 14:32:10\u0000".dropLast(1).plus("\u0000").let { "\u0000" }))
    }

    @Test
    fun `trailing nul bytes and whitespace are trimmed`() {
        assertEquals("2026-10-06 14:32:10", ExifDateParser.normalize(" 2026:10:06 14:32:10\u0000"))
    }

    @Test
    fun `unknown formats are passed through trimmed`() {
        assertEquals("06.10.2026 14:32", ExifDateParser.normalize(" 06.10.2026 14:32 "))
    }

    @Test
    fun `pick prefers original then digitized then modified`() {
        assertEquals("2020-01-01 00:00:01", ExifDateParser.pick("2020:01:01 00:00:01", "2020:01:01 00:00:02", "2020:01:01 00:00:03"))
        assertEquals("2020-01-01 00:00:02", ExifDateParser.pick(null, "2020:01:01 00:00:02", "2020:01:01 00:00:03"))
        assertEquals("2020-01-01 00:00:03", ExifDateParser.pick("", "    :  :     :  :  ", "2020:01:01 00:00:03"))
        assertNull(ExifDateParser.pick(null, null, null))
    }
}
