/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.platform.time

import kotlin.math.ceil

private var currentToday: LocalDate? = null

fun getToday(): LocalDate =
    currentToday ?: error("getToday() called before setToday()")

fun setToday(date: LocalDate) {
    currentToday = date
}

fun resetToday() {
    currentToday = null
}

private var currentHijriOffset: Int = 0

fun getHijriOffset(): Int = currentHijriOffset

fun setHijriOffset(offset: Int) {
    currentHijriOffset = offset.coerceIn(-2, 2)
}

fun resetHijriOffset() {
    currentHijriOffset = 0
}

val HIJRI_SHORT_MONTH_NAMES = arrayOf(
    "محرم",
    "صفر",
    "ربيع ١",
    "ربيع ٢",
    "جمادى ١",
    "جمادى ٢",
    "رجب",
    "شعبان",
    "رمضان",
    "شوال",
    "ذو القعدة",
    "ذو الحجة"
)

val HIJRI_LONG_MONTH_NAMES = arrayOf(
    "محرم",
    "صفر",
    "ربيع الأول",
    "ربيع الآخر",
    "جمادى الأولى",
    "جمادى الآخرة",
    "رجب",
    "شعبان",
    "رمضان",
    "شوال",
    "ذو القعدة",
    "ذو الحجة"
)

private var currentFirstWeekday: DayOfWeek? = null

fun getFirstWeekday(): DayOfWeek =
    currentFirstWeekday ?: DayOfWeek.entries[getFirstWeekdayNumberAccordingToLocale() - 1]

fun setFirstWeekday(firstWeekday: DayOfWeek) {
    currentFirstWeekday = firstWeekday
}

fun resetFirstWeekday() {
    currentFirstWeekday = null
}

enum class DayOfWeek(val daysSinceSunday: Int) {
    SUNDAY(0),
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6)
}

data class LocalDate(val daysSince2000: Int) : Comparable<LocalDate> {
    override fun compareTo(other: LocalDate): Int =
        daysSince2000.compareTo(other.daysSince2000)

    var yearCache = -1
    var monthCache = -1
    var dayCache = -1
    private var hijriYearCache = -1
    private var hijriMonthCache = -1
    private var hijriDayCache = -1
    private var hijriOffsetCache = Int.MIN_VALUE

    constructor(year: Int, month: Int, day: Int) :
        this(daysSince2000(year, month, day))

    val dayOfWeek: DayOfWeek
        get() {
            val rem = daysSince2000 % 7
            val mod = if (rem < 0) rem + 7 else rem
            return when (mod) {
                0 -> DayOfWeek.SATURDAY
                1 -> DayOfWeek.SUNDAY
                2 -> DayOfWeek.MONDAY
                3 -> DayOfWeek.TUESDAY
                4 -> DayOfWeek.WEDNESDAY
                5 -> DayOfWeek.THURSDAY
                else -> DayOfWeek.FRIDAY
            }
        }

    val year: Int
        get() {
            if (yearCache < 0) updateYearMonthDayCache()
            return yearCache
        }

    val month: Int
        get() {
            if (monthCache < 0) updateYearMonthDayCache()
            return monthCache
        }

    val day: Int
        get() {
            if (dayCache < 0) updateYearMonthDayCache()
            return dayCache
        }

    val monthLength: Int
        get() = when (month) {
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 31
        }

    val yearLength: Int
        get() = if (isLeapYear(year)) 366 else 365

    val hijriYear: Int
        get() {
            if (hijriYearCache < 0 || hijriOffsetCache != currentHijriOffset) updateHijriCache()
            return hijriYearCache
        }

    val hijriMonth: Int
        get() {
            if (hijriMonthCache < 0 || hijriOffsetCache != currentHijriOffset) updateHijriCache()
            return hijriMonthCache
        }

    val hijriDay: Int
        get() {
            if (hijriDayCache < 0 || hijriOffsetCache != currentHijriOffset) updateHijriCache()
            return hijriDayCache
        }

    val hijriMonthLength: Int
        get() = hijriMonthLength(hijriYear, hijriMonth)

    val hijriYearLength: Int
        get() = hijriYearLength(hijriYear)

    private fun updateYearMonthDayCache() {
        var currYear = 2000
        var currDay = 0
        if (daysSince2000 < 0) {
            currYear -= 400
            currDay -= 146097
        }
        while (true) {
            val currYearLength = if (isLeapYear(currYear)) 366 else 365
            if (daysSince2000 < currDay + currYearLength) {
                yearCache = currYear
                break
            } else {
                currYear++
                currDay += currYearLength
            }
        }
        var currMonth = 1
        val monthOffset = if (isLeapYear(currYear)) leapOffset else nonLeapOffset
        while (true) {
            if (daysSince2000 < currDay + monthOffset[currMonth]) {
                monthCache = currMonth
                break
            } else {
                currMonth++
            }
        }
        currDay += monthOffset[currMonth - 1]
        dayCache = daysSince2000 - currDay + 1
    }

    private fun updateHijriCache() {
        val effectiveDays = daysSince2000 + currentHijriOffset
        var currYear: Int
        var currDay: Int
        val tableMinDays = HIJRI_YEAR_START_DAYS[0]
        val tableMaxDays = HIJRI_YEAR_START_DAYS[HIJRI_TABLE_MAX_YEAR - HIJRI_TABLE_MIN_YEAR + 1]
        if (effectiveDays in tableMinDays until tableMaxDays) {
            var low = 0
            var high = HIJRI_TABLE_MAX_YEAR - HIJRI_TABLE_MIN_YEAR
            while (low <= high) {
                val mid = (low + high) ushr 1
                if (effectiveDays >= HIJRI_YEAR_START_DAYS[mid + 1]) {
                    low = mid + 1
                } else if (effectiveDays < HIJRI_YEAR_START_DAYS[mid]) {
                    high = mid - 1
                } else {
                    low = mid
                    break
                }
            }
            currYear = HIJRI_TABLE_MIN_YEAR + low
            currDay = HIJRI_YEAR_START_DAYS[low]
        } else if (effectiveDays >= tableMaxDays) {
            currYear = HIJRI_TABLE_MAX_YEAR + 1
            currDay = tableMaxDays
            while (true) {
                val len = hijriYearLength(currYear)
                if (effectiveDays < currDay + len) break
                currDay += len
                currYear++
            }
        } else {
            currYear = HIJRI_TABLE_MIN_YEAR - 1
            currDay = tableMinDays - hijriYearLength(currYear)
            while (effectiveDays < currDay) {
                currYear--
                currDay -= hijriYearLength(currYear)
            }
        }
        hijriYearCache = currYear
        var currMonth = 1
        while (currMonth < 12) {
            val mLen = hijriMonthLength(currYear, currMonth)
            if (effectiveDays < currDay + mLen) {
                break
            }
            currDay += mLen
            currMonth++
        }
        hijriMonthCache = currMonth
        hijriDayCache = effectiveDays - currDay + 1
        hijriOffsetCache = currentHijriOffset
    }

    fun isOlderThan(other: LocalDate): Boolean {
        return daysSince2000 < other.daysSince2000
    }

    fun isNewerThan(other: LocalDate): Boolean {
        return daysSince2000 > other.daysSince2000
    }

    fun plus(days: Int): LocalDate {
        return LocalDate(daysSince2000 + days)
    }

    fun minus(days: Int): LocalDate {
        return LocalDate(daysSince2000 - days)
    }

    fun daysUntil(other: LocalDate): Int {
        return other.daysSince2000 - this.daysSince2000
    }

    val unixTime: Long
        get() = EPOCH_2000_MILLIS + daysSince2000.toLong() * MILLIS_PER_DAY

    fun startOfWeek(firstWeekday: DayOfWeek): LocalDate {
        var delta = dayOfWeek.daysSinceSunday - firstWeekday.daysSinceSunday
        if (delta < 0) delta += 7
        return minus(delta)
    }

    fun startOfMonth(): LocalDate = LocalDate(year, month, 1)

    fun startOfQuarter(): LocalDate {
        val quarterMonth = ((month - 1) / 3) * 3 + 1
        return LocalDate(year, quarterMonth, 1)
    }

    fun startOfYear(): LocalDate = LocalDate(year, 1, 1)

    fun startOfHijriMonth(): LocalDate = fromHijri(hijriYear, hijriMonth, 1)

    fun startOfHijriQuarter(): LocalDate {
        val quarterMonth = ((hijriMonth - 1) / 3) * 3 + 1
        return fromHijri(hijriYear, quarterMonth, 1)
    }

    fun startOfHijriYear(): LocalDate = fromHijri(hijriYear, 1, 1)

    fun toCSVString(): String {
        val y = year.toString().padStart(4, '0')
        val m = month.toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    fun toHijriCSVString(): String {
        val y = hijriYear.toString().padStart(4, '0')
        val m = hijriMonth.toString().padStart(2, '0')
        val d = hijriDay.toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    override fun toString(): String {
        return "LocalDate($year-$month-$day)"
    }

    companion object {
        private const val EPOCH_2000_MILLIS = 946684800000L
        private const val MILLIS_PER_DAY = 86400000L

        fun fromUnixTime(millis: Long): LocalDate {
            val diff = millis - EPOCH_2000_MILLIS
            val days = if (diff >= 0) diff / MILLIS_PER_DAY else (diff - MILLIS_PER_DAY + 1) / MILLIS_PER_DAY
            return LocalDate(days.toInt())
        }

        fun fromHijri(year: Int, month: Int, day: Int): LocalDate {
            return LocalDate(hijriDaysSince2000(year, month, day))
        }
    }
}

interface LocalDateFormatter {
    fun narrowWeekdayName(weekday: DayOfWeek): String
    fun narrowWeekdayName(date: LocalDate): String
    fun shortWeekdayName(weekday: DayOfWeek): String
    fun shortWeekdayName(date: LocalDate): String
    fun shortMonthName(date: LocalDate): String
    fun longWeekdayName(weekday: DayOfWeek): String
    fun longMonthName(date: LocalDate): String
}

private const val HIJRI_TABLE_MIN_YEAR = 1300
private const val HIJRI_TABLE_MAX_YEAR = 1600
private const val HIJRI_MIN_YEAR_DAYS_SINCE_2000 = -42783

private val UMM_AL_QURA_MONTH_BITS = intArrayOf(
    0x555, 0x2ab, 0x937, 0x2b6, 0x576, 0x36c, 0xb55, 0xaaa, 0x956, 0x49e,
    0x95d, 0x2ba, 0x5b5, 0x3aa, 0xb4b, 0xa96, 0x52e, 0x2ad, 0x56d, 0xb5a,
    0x752, 0xf25, 0xe8a, 0xd16, 0xa56, 0xab5, 0x6b4, 0xda9, 0xb92, 0xb25,
    0x64b, 0xa9b, 0x35a, 0x6d9, 0x5d4, 0xda5, 0xd4a, 0xa95, 0x536, 0x975,
    0x2f4, 0x6e9, 0x6d4, 0x6a9, 0x535, 0x25d, 0x4bd, 0x9ba, 0x3b4, 0xb69,
    0xb2a, 0xa55, 0x4ad, 0xa5d, 0x2da, 0x6d9, 0xeaa, 0xe94, 0xd2a, 0xc56,
    0x4ae, 0xa6d, 0x56a, 0xd55, 0xd4a, 0xa93, 0x52b, 0xa5b, 0x53a, 0x6b5,
    0xea9, 0xd52, 0xd29, 0xa55, 0x4ad, 0x56d, 0xaea, 0x6e4, 0xed1, 0xda2,
    0xaaa, 0x95a, 0x2da, 0x5b9, 0xbb2, 0x764, 0x6c9, 0x555, 0x2ab, 0x4db,
    0xaba, 0x5b4, 0xda9, 0xd52, 0xaa5, 0x92d, 0x26d, 0x8ed, 0x2da, 0xad5,
    0xaa5, 0xa4b, 0x497, 0x937, 0x2b6, 0x975, 0xd69, 0xd52, 0xc95, 0x92b,
    0x25b, 0x4db, 0x9d5, 0x5d2, 0xda5, 0xd4a, 0xa95, 0x54d, 0xaad, 0x3aa,
    0xbd2, 0xbc4, 0xb89, 0xa95, 0x52d, 0x5ad, 0xb6a, 0x6d4, 0xdc9, 0xd92,
    0xaa6, 0x956, 0x2ae, 0x56d, 0x36a, 0xb55, 0xaaa, 0x94d, 0x49d, 0x95d,
    0x2ba, 0x5b5, 0x5aa, 0xd55, 0xa9a, 0x92e, 0x26e, 0x55d, 0xada, 0x6d4,
    0x6a5, 0xb27, 0xa4d, 0x4ad, 0x56d, 0xb5a, 0x754, 0xf49, 0xe92, 0xd26,
    0xa56, 0x356, 0x6b5, 0xbaa, 0xb92, 0xb25, 0x68b, 0xa9b, 0x55a, 0xada,
    0x5b4, 0xda9, 0xb52, 0xa9a, 0x536, 0x276, 0x575, 0xaf2, 0x6d4, 0x6a9,
    0x555, 0x2ad, 0x4bd, 0x9ba, 0x574, 0xb69, 0xb52, 0xa95, 0x52d, 0xa5d,
    0x4da, 0xad9, 0x6b2, 0xe95, 0xe2a, 0xc96, 0x92e, 0xaad, 0x56a, 0xd65,
    0xd4a, 0xd15, 0x62b, 0xc5b, 0x53a, 0x6b5, 0xdb2, 0xd64, 0xd29, 0xa55,
    0x4ad, 0x96d, 0xaea, 0x6e8, 0xed1, 0xda4, 0xd4a, 0xa6a, 0x2da, 0x5b9,
    0xb72, 0xb68, 0x6d1, 0x655, 0x4ab, 0x95b, 0x2ba, 0x5b5, 0xda9, 0xd52,
    0xca6, 0x94e, 0x46e, 0x95d, 0x4da, 0xad5, 0xaaa, 0xa4d, 0x49b, 0x937,
    0x4b6, 0x975, 0xd6a, 0xd52, 0xaa5, 0x94b, 0x2ab, 0x55b, 0xad9, 0x5d2,
    0xdc5, 0xd92, 0xb25, 0x555, 0xab5, 0x5b4, 0xba9, 0x7a2, 0x745, 0x593,
    0xaab, 0x4d6, 0x9d6, 0x5d2, 0xba5, 0xb4a, 0xa95, 0x4ad, 0x15d, 0x2dd,
    0x9da, 0x5b4, 0x5a9, 0x52d, 0x25b, 0x8b7, 0x176, 0x56d, 0xb6a, 0xaca,
    0xa96, 0x52b, 0x15b, 0x2bb, 0x5b6, 0xdaa, 0xb94, 0xd46, 0xa8d, 0x52d,
    0xa9d, 0x55a, 0x755, 0x749, 0xf13, 0xe4a, 0xa96, 0x556, 0x6b5, 0xbaa,
    0xb94
)

private fun isTabularHijriLeapYear(year: Int): Boolean {
    val rem = (11 * year + 14) % 30
    val mod = if (rem < 0) rem + 30 else rem
    return mod < 11
}

fun hijriMonthLength(year: Int, month: Int): Int {
    val m = month.coerceIn(1, 12)
    if (year in HIJRI_TABLE_MIN_YEAR..HIJRI_TABLE_MAX_YEAR) {
        val bits = UMM_AL_QURA_MONTH_BITS[year - HIJRI_TABLE_MIN_YEAR]
        return 29 + ((bits shr (m - 1)) and 1)
    }
    return when {
        m % 2 == 1 -> 30
        m == 12 && isTabularHijriLeapYear(year) -> 30
        else -> 29
    }
}

fun hijriYearLength(year: Int): Int {
    if (year in HIJRI_TABLE_MIN_YEAR..HIJRI_TABLE_MAX_YEAR) {
        var sum = 0
        val bits = UMM_AL_QURA_MONTH_BITS[year - HIJRI_TABLE_MIN_YEAR]
        for (m in 0 until 12) {
            sum += 29 + ((bits shr m) and 1)
        }
        return sum
    }
    return if (isTabularHijriLeapYear(year)) 355 else 354
}

private val HIJRI_YEAR_START_DAYS: IntArray = run {
    val count = HIJRI_TABLE_MAX_YEAR - HIJRI_TABLE_MIN_YEAR + 1
    val arr = IntArray(count + 1)
    arr[0] = HIJRI_MIN_YEAR_DAYS_SINCE_2000
    for (i in 0 until count) {
        arr[i + 1] = arr[i] + hijriYearLength(HIJRI_TABLE_MIN_YEAR + i)
    }
    arr
}

private fun rawDaysSince2000(year: Int, month: Int, day: Int): Int {
    var currDay: Int
    if (year in HIJRI_TABLE_MIN_YEAR..HIJRI_TABLE_MAX_YEAR) {
        currDay = HIJRI_YEAR_START_DAYS[year - HIJRI_TABLE_MIN_YEAR]
    } else if (year > HIJRI_TABLE_MAX_YEAR) {
        currDay = HIJRI_YEAR_START_DAYS[HIJRI_TABLE_MAX_YEAR - HIJRI_TABLE_MIN_YEAR + 1]
        var y = HIJRI_TABLE_MAX_YEAR + 1
        while (y < year) {
            currDay += hijriYearLength(y)
            y++
        }
    } else {
        currDay = HIJRI_YEAR_START_DAYS[0]
        var y = HIJRI_TABLE_MIN_YEAR - 1
        while (y >= year) {
            currDay -= hijriYearLength(y)
            y--
        }
    }
    val m = month.coerceIn(1, 12)
    for (currMonth in 1 until m) {
        currDay += hijriMonthLength(year, currMonth)
    }
    currDay += (day - 1)
    return currDay
}

private fun hijriDaysSince2000(year: Int, month: Int, day: Int): Int {
    return rawDaysSince2000(year, month, day) - currentHijriOffset
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}

val leapOffset = arrayOf(
    0, 31, 60, 91, 121, 152, 182,
    213, 244, 274, 305, 335, 366
)
val nonLeapOffset = arrayOf(
    0, 31, 59, 90, 120, 151, 181,
    212, 243, 273, 304, 334, 365
)

private fun daysSince2000(year: Int, month: Int, day: Int): Int {
    var result = 365 * (year - 2000)
    result += ceil((year - 2000) / 4.0).toInt()
    result -= ceil((year - 2000) / 100.0).toInt()
    result += ceil((year - 2000) / 400.0).toInt()
    result += if (isLeapYear(year)) {
        leapOffset[month - 1]
    } else {
        nonLeapOffset[month - 1]
    }
    result += (day - 1)
    return result
}

enum class TruncateField {
    DAY, WEEK_NUMBER, MONTH, QUARTER, YEAR
}

fun getWeekdaySequence(firstWeekday: DayOfWeek): List<DayOfWeek> {
    val allDays = DayOfWeek.entries
    return (0 until 7).map { offset ->
        allDays[(firstWeekday.daysSinceSunday + offset) % 7]
    }
}

expect fun getFirstWeekdayNumberAccordingToLocale(): Int

expect fun computeToday(hourOffset: Int = 0, minuteOffset: Int = 0): LocalDate

fun countWeekdayOccurrencesInMonth(startOfMonth: LocalDate): Array<Int> {
    val weekday = (startOfMonth.dayOfWeek.daysSinceSunday + 1) % 7
    val freq = Array(7) { 0 }
    for (day in weekday until weekday + startOfMonth.monthLength) {
        freq[day % 7] += 1
    }
    return freq
}

fun countWeekdayOccurrencesInHijriMonth(startOfHijriMonth: LocalDate): Array<Int> {
    val weekday = (startOfHijriMonth.dayOfWeek.daysSinceSunday + 1) % 7
    val freq = Array(7) { 0 }
    for (day in weekday until weekday + startOfHijriMonth.hijriMonthLength) {
        freq[day % 7] += 1
    }
    return freq
}
