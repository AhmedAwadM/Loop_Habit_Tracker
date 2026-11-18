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

import kotlinx.coroutines.test.runTest
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.platform.time.resetHijriOffset
import org.isoron.platform.time.setHijriOffset
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.PaletteColor
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HabitsCSVExporterTest : BaseUnitTest() {

    @BeforeTest
    override fun setUp() {
        super.setUp()
        val today = getToday()

        val meditate = fixtures.createEmptyHabit()
        meditate.position = 1

        val short = fixtures.createShortHabit()
        short.position = 2

        val reading = modelFactory.buildHabit()
        reading.name = "Reading"
        reading.question = "Did you read today?"
        reading.type = HabitType.YES_NO
        reading.frequency = Frequency.DAILY
        reading.color = PaletteColor(6)
        reading.position = 3
        reading.category = "Books"

        val stretch = modelFactory.buildHabit()
        stretch.name = "Stretch"
        stretch.question = "Did you stretch today?"
        stretch.type = HabitType.YES_NO
        stretch.frequency = Frequency.DAILY
        stretch.color = PaletteColor(10)
        stretch.position = 4
        stretch.category = "Health, Wellness"

        val commaName = modelFactory.buildHabit()
        commaName.name = "Foo, Bar"
        commaName.question = "Did you do foo bar?"
        commaName.type = HabitType.YES_NO
        commaName.frequency = Frequency.DAILY
        commaName.color = PaletteColor(12)
        commaName.position = 5
        commaName.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        commaName.recompute()

        val nonDaily = modelFactory.buildHabit()
        nonDaily.name = "Night run"
        nonDaily.question = "Did you run at night?"
        nonDaily.type = HabitType.YES_NO
        nonDaily.frequency = Frequency(1, 3)
        nonDaily.color = PaletteColor(14)
        nonDaily.position = 6
        nonDaily.originalEntries.add(Entry(today.minus(4), Entry.YES_MANUAL))
        nonDaily.originalEntries.add(Entry(today.minus(9), Entry.YES_MANUAL))
        nonDaily.originalEntries.add(Entry(today.minus(10), Entry.YES_MANUAL))
        nonDaily.recompute()

        habitList.add(meditate)
        habitList.add(short)
        habitList.add(reading)
        habitList.add(stretch)
        habitList.add(commaName)
        habitList.add(nonDaily)
    }

    @Test
    fun testExportCSV() = runTest {
        val today = getToday()
        val numHabit = modelFactory.buildHabit().apply {
            name = "Pushups"
            type = HabitType.NUMERICAL
            frequency = Frequency.DAILY
            position = 7
            originalEntries.add(Entry(today, 1000)) // user entered 1
            originalEntries.add(Entry(today.minus(1), 100000)) // user entered 100
            originalEntries.add(Entry(today.minus(2), Entry.SKIP)) // skip -> empty
            originalEntries.add(Entry(today.minus(3), 12500)) // user entered 12.5
            recompute()
        }
        habitList.add(numHabit)

        val boolHabit = modelFactory.buildHabit().apply {
            name = "Pray"
            type = HabitType.YES_NO
            frequency = Frequency.DAILY
            position = 8
            originalEntries.add(Entry(today, Entry.YES_MANUAL)) // 1
            originalEntries.add(Entry(today.minus(1), Entry.NO)) // 0
            originalEntries.add(Entry(today.minus(2), Entry.SKIP)) // empty
            recompute()
        }
        habitList.add(boolHabit)

        val nonDaily = habitList.find { it.name == "Night run" }!!
        val commaHabit = habitList.find { it.name == "Foo, Bar" }!!

        val selected = listOf(boolHabit, numHabit, commaHabit, nonDaily)
        val exporter = HabitsCSVExporter(habitList, selected, today.minus(4), today)
        val csv = exporter.writeCSV()
        val lines = csv.trimEnd().lines()
        assertEquals(6, lines.size)
        assertEquals("Date,Pray,Pushups,\"Foo, Bar\",Night run", lines[0])
        assertEquals("${today.toHijriCSVString()},1,1,1,0", lines[1])
        assertEquals("${today.minus(1).toHijriCSVString()},0,100,0,0", lines[2])
        assertEquals("${today.minus(2).toHijriCSVString()},,,0,0", lines[3])
        assertEquals("${today.minus(3).toHijriCSVString()},0,12.5,0,0", lines[4])
        assertEquals("${today.minus(4).toHijriCSVString()},0,0,0,1", lines[5])
    }

    @Test
    fun testExportCSVWithHijriOffsetAndRanges() = runTest {
        try {
            setHijriOffset(-1)
            val date = LocalDate(2026, 11, 18) // 1448-06-08 -> with -1 offset = 1448-06-07
            val habit = modelFactory.buildHabit().apply {
                name = "Quran"
                type = HabitType.NUMERICAL
                frequency = Frequency.DAILY
                originalEntries.add(Entry(date, 5000))
                recompute()
            }
            val weekCsv = HabitsCSVExporter(habitList, listOf(habit), date.minus(6), date).writeCSV()
            val weekLines = weekCsv.trimEnd().lines()
            assertEquals(8, weekLines.size)
            assertEquals("Date,Quran", weekLines[0])
            assertEquals("1448-06-07,5", weekLines[1])
            assertEquals("1448-06-01,0", weekLines[7])

            val monthCsv = HabitsCSVExporter(habitList, listOf(habit), date.minus(29), date).writeCSV()
            assertEquals(31, monthCsv.trimEnd().lines().size)

            val yearCsv = HabitsCSVExporter(habitList, listOf(habit), date.minus(date.hijriYearLength - 1), date).writeCSV()
            assertEquals(date.hijriYearLength + 1, yearCsv.trimEnd().lines().size)
        } finally {
            resetHijriOffset()
        }
    }
}
