package org.isoron.platform.time

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaLocalDateFormatterTest {
    private val formatter = JavaLocalDateFormatter(Locale.US)

    @Test
    fun testLongFormat() {
        assertEquals("1 محرم 1448", formatter.longFormat(LocalDate.fromHijri(1448, 1, 1)))
        assertEquals("15 رمضان 1447", formatter.longFormat(LocalDate.fromHijri(1447, 9, 15)))
        assertEquals("24 رمضان 1420", formatter.longFormat(LocalDate(2000, 1, 1)))
    }

    @Test
    fun testShortWeekdayNames() {
        val names = formatter.shortWeekdayNames(DayOfWeek.SUNDAY)
        assertEquals(7, names.size)
        assertEquals("Sun", names[0])
        assertEquals("Mon", names[1])
        assertEquals("Tue", names[2])
        assertEquals("Wed", names[3])
        assertEquals("Thu", names[4])
        assertEquals("Fri", names[5])
        assertEquals("Sat", names[6])
    }

    @Test
    fun testShortWeekdayNames_startingMonday() {
        val names = formatter.shortWeekdayNames(DayOfWeek.MONDAY)
        assertEquals("Mon", names[0])
        assertEquals("Sun", names[6])
    }

    @Test
    fun testLongWeekdayNames() {
        val names = formatter.longWeekdayNames(DayOfWeek.SUNDAY)
        assertEquals(7, names.size)
        assertEquals("Sunday", names[0])
        assertEquals("Monday", names[1])
        assertEquals("Tuesday", names[2])
        assertEquals("Wednesday", names[3])
        assertEquals("Thursday", names[4])
        assertEquals("Friday", names[5])
        assertEquals("Saturday", names[6])
    }

    @Test
    fun testLongWeekdayNames_startingSaturday() {
        val names = formatter.longWeekdayNames(DayOfWeek.SATURDAY)
        assertEquals("Saturday", names[0])
        assertEquals("Friday", names[6])
    }
}
