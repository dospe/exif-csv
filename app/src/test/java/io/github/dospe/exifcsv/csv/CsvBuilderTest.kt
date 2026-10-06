package io.github.dospe.exifcsv.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvBuilderTest {

    private val record = PhotoRecord(
        name = "IMG_0001.jpg",
        dateTime = "2026-10-06 14:32:10",
        latitude = 50.0875,
        longitude = 14.4213,
        altitude = 235.4,
    )

    @Test
    fun `writes bom, header and rows with semicolons and crlf`() {
        val csv = CsvBuilder.build(listOf(record))
        val expected = CsvBuilder.BOM +
            "nazev;datum_cas;lat;lon;alt\r\n" +
            "IMG_0001.jpg;2026-10-06 14:32:10;50.087500;14.421300;235.4\r\n"
        assertEquals(expected, csv)
    }

    @Test
    fun `decimal comma option replaces dots in numbers only`() {
        val csv = CsvBuilder.build(listOf(record), CsvOptions(decimalComma = true, includeBom = false))
        assertEquals(
            "nazev;datum_cas;lat;lon;alt\r\n" +
                "IMG_0001.jpg;2026-10-06 14:32:10;50,087500;14,421300;235,4\r\n",
            csv,
        )
    }

    @Test
    fun `missing values become empty cells`() {
        val csv = CsvBuilder.build(
            listOf(PhotoRecord("a.jpg", null, null, null, null)),
            CsvOptions(includeBom = false),
        )
        assertEquals("nazev;datum_cas;lat;lon;alt\r\na.jpg;;;;\r\n", csv)
    }

    @Test
    fun `comma delimiter is supported`() {
        val csv = CsvBuilder.build(listOf(record), CsvOptions(delimiter = ',', includeBom = false))
        assertEquals(
            "nazev,datum_cas,lat,lon,alt\r\nIMG_0001.jpg,2026-10-06 14:32:10,50.087500,14.421300,235.4\r\n",
            csv,
        )
    }

    @Test
    fun `values containing delimiter quotes or newlines are quoted`() {
        assertEquals("plain.jpg", CsvBuilder.escape("plain.jpg", ';'))
        assertEquals("\"a;b.jpg\"", CsvBuilder.escape("a;b.jpg", ';'))
        assertEquals("\"say \"\"hi\"\".jpg\"", CsvBuilder.escape("say \"hi\".jpg", ';'))
        assertEquals("\"line\nbreak\"", CsvBuilder.escape("line\nbreak", ';'))
        assertEquals("a,b", CsvBuilder.escape("a,b", ';'))
    }

    @Test
    fun `negative coordinates keep their sign and precision`() {
        assertEquals("-33.868820", CsvBuilder.formatNumber(-33.86882, 6, false))
        assertEquals("151.209300", CsvBuilder.formatNumber(151.2093, 6, false))
        assertEquals("-12,5", CsvBuilder.formatNumber(-12.5, 1, true))
        assertEquals("", CsvBuilder.formatNumber(Double.NaN, 1, false))
        assertEquals("", CsvBuilder.formatNumber(null, 1, false))
    }
}
