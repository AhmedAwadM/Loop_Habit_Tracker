package org.isoron.platform.time

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HijriDatesTest {

    @AfterTest
    fun tearDown() {
        resetHijriOffset()
    }

    @Test
    fun testKnownHijriDates() {
        val epoch2000 = LocalDate(2000, 1, 1)
        assertEquals(1420, epoch2000.hijriYear)
        assertEquals(9, epoch2000.hijriMonth)
        assertEquals(24, epoch2000.hijriDay)
        assertEquals("1420-09-24", epoch2000.toHijriCSVString())

        val sample = LocalDate(2026, 11, 18)
        assertEquals(1448, sample.hijriYear)
        assertEquals(6, sample.hijriMonth)
        assertEquals(8, sample.hijriDay)
        assertEquals("1448-06-08", sample.toHijriCSVString())
    }

    @Test
    fun testHijriOffsetsAndRoundTrip() {
        val base = LocalDate(2026, 11, 18) // 1448-06-08, Wednesday
        val originalDayOfWeek = base.dayOfWeek
        val originalUnixTime = base.unixTime

        val expectedDays = mapOf(
            -2 to "1448-06-06",
            -1 to "1448-06-07",
            0 to "1448-06-08",
            1 to "1448-06-09",
            2 to "1448-06-10"
        )

        for ((offset, expectedStr) in expectedDays) {
            setHijriOffset(offset)
            assertEquals(offset, getHijriOffset())
            assertEquals(expectedStr, base.toHijriCSVString())
            assertEquals(originalDayOfWeek, base.dayOfWeek)
            assertEquals(originalUnixTime, base.unixTime)
            val reconstructed = LocalDate.fromHijri(base.hijriYear, base.hijriMonth, base.hijriDay)
            assertEquals(base, reconstructed)
        }

        // Clamping beyond [-2, 2]
        setHijriOffset(5)
        assertEquals(2, getHijriOffset())
        setHijriOffset(-5)
        assertEquals(-2, getHijriOffset())
    }

    @Test
    fun testHijriMonthQuarterYearStarts() {
        val d = LocalDate.fromHijri(1448, 5, 15)
        assertEquals(LocalDate.fromHijri(1448, 5, 1), d.startOfHijriMonth())
        assertEquals(LocalDate.fromHijri(1448, 4, 1), d.startOfHijriQuarter())
        assertEquals(LocalDate.fromHijri(1448, 1, 1), d.startOfHijriYear())
        assertEquals(355, LocalDate.fromHijri(1448, 1, 1).hijriYearLength)
        assertEquals(354, LocalDate.fromHijri(1445, 1, 1).hijriYearLength)
    }
}
