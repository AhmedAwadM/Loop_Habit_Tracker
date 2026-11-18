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
package org.isoron.uhabits.core.io

import org.isoron.platform.io.csvLine
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList

/**
 * Class that exports the application data to a single CSV file.
 */
class HabitsCSVExporter(
    private val allHabits: HabitList,
    private val selectedHabits: List<Habit>,
    private val startDate: LocalDate? = null,
    private val endDate: LocalDate? = null
) {
    fun writeCSV(): String {
        val sb = StringBuilder()
        val headerFields = arrayOf("Date") + selectedHabits.map { it.name }.toTypedArray()
        sb.append(csvLine(headerFields))

        val timeframe = getTimeframe()
        val oldest = timeframe[0]
        val newest = timeframe[1]
        val days = oldest.daysUntil(newest)
        for (i in 0..days) {
            val currentDate = newest.minus(i)
            val rowFields = Array(selectedHabits.size + 1) { index ->
                if (index == 0) {
                    currentDate.toHijriCSVString()
                } else {
                    val habit = selectedHabits[index - 1]
                    val entry = habit.originalEntries.get(currentDate)
                    formatEntryValue(habit, entry)
                }
            }
            sb.append(csvLine(rowFields))
        }
        return sb.toString()
    }

    private fun formatEntryValue(habit: Habit, entry: Entry): String {
        if (entry.value == Entry.SKIP) {
            return ""
        }
        return if (habit.isNumerical) {
            if (entry.value == Entry.UNKNOWN || entry.value <= 0) {
                "0"
            } else if (entry.value % 1000 == 0) {
                (entry.value / 1000).toString()
            } else {
                val formatted = (entry.value / 1000.0).toString()
                if (formatted.contains('.')) {
                    formatted.trimEnd('0').trimEnd('.')
                } else {
                    formatted
                }
            }
        } else {
            if (entry.value == Entry.YES_MANUAL) "1" else "0"
        }
    }

    private fun getTimeframe(): Array<LocalDate> {
        if (startDate != null && endDate != null) {
            return if (startDate.isOlderThan(endDate)) {
                arrayOf(startDate, endDate)
            } else {
                arrayOf(endDate, startDate)
            }
        }
        val today = getToday()
        var oldest = today
        var newest = today
        var foundAny = false
        for (habit in selectedHabits) {
            val entries = habit.computedEntries.getKnown()
            if (entries.isEmpty()) continue
            val currNew = entries[0].date
            val currOld = entries[entries.size - 1].date
            if (!foundAny) {
                oldest = currOld
                newest = if (currNew.isNewerThan(today)) currNew else today
                foundAny = true
            } else {
                if (currOld.isOlderThan(oldest)) oldest = currOld
                if (currNew.isNewerThan(newest)) newest = currNew
            }
        }
        return arrayOf(oldest, newest)
    }
}
