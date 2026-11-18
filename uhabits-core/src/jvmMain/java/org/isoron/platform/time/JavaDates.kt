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

import java.util.Calendar.DAY_OF_WEEK
import java.util.Calendar.LONG
import java.util.Calendar.NARROW_FORMAT
import java.util.Calendar.SHORT
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

fun LocalDate.toGregorianCalendar(): GregorianCalendar {
    val cal = GregorianCalendar(TimeZone.getTimeZone("GMT"))
    cal.timeInMillis = this.unixTime
    return cal
}

actual fun getFirstWeekdayNumberAccordingToLocale(): Int {
    return GregorianCalendar(Locale.getDefault()).firstDayOfWeek
}

class JavaLocalDateFormatter(private val locale: Locale) : LocalDateFormatter {
    override fun shortMonthName(date: LocalDate): String {
        val m = date.hijriMonth.coerceIn(1, 12)
        return HIJRI_SHORT_MONTH_NAMES[m - 1]
    }

    override fun shortWeekdayName(weekday: DayOfWeek): String {
        val cal = GregorianCalendar()
        cal.set(DAY_OF_WEEK, weekday.daysSinceSunday + 1)
        return cal.getDisplayName(DAY_OF_WEEK, SHORT, locale)
    }

    override fun shortWeekdayName(date: LocalDate): String {
        val cal = date.toGregorianCalendar()
        return cal.getDisplayName(DAY_OF_WEEK, SHORT, locale)
    }

    override fun narrowWeekdayName(weekday: DayOfWeek): String {
        val cal = GregorianCalendar()
        cal.set(DAY_OF_WEEK, weekday.daysSinceSunday + 1)
        return cal.getDisplayName(DAY_OF_WEEK, SHORT, locale)
    }

    override fun narrowWeekdayName(date: LocalDate): String {
        val cal = date.toGregorianCalendar()
        return cal.getDisplayName(DAY_OF_WEEK, NARROW_FORMAT, locale)
    }

    override fun longWeekdayName(weekday: DayOfWeek): String {
        val cal = GregorianCalendar()
        cal.set(DAY_OF_WEEK, weekday.daysSinceSunday + 1)
        return cal.getDisplayName(DAY_OF_WEEK, LONG, locale)
    }

    override fun longMonthName(date: LocalDate): String {
        val m = date.hijriMonth.coerceIn(1, 12)
        return HIJRI_LONG_MONTH_NAMES[m - 1]
    }

    fun longFormat(date: LocalDate): String {
        return "${date.hijriDay} ${shortMonthName(date)} ${date.hijriYear}"
    }

    fun shortWeekdayNames(firstWeekday: DayOfWeek): Array<String> {
        return getWeekdaySequence(firstWeekday).map { shortWeekdayName(it) }.toTypedArray()
    }

    fun longWeekdayNames(firstWeekday: DayOfWeek): Array<String> {
        return getWeekdaySequence(firstWeekday).map { longWeekdayName(it) }.toTypedArray()
    }
}
