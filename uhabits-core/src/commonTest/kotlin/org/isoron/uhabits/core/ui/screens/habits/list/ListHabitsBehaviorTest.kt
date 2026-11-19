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
package org.isoron.uhabits.core.ui.screens.habits.list

import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.UserFile
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.platform.time.resetFirstWeekday
import org.isoron.platform.time.resetToday
import org.isoron.platform.time.setFirstWeekday
import org.isoron.platform.time.setToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.callbacks.NumberPickerCallback
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import dev.mokkery.verify.VerifyMode.Companion.not as notCalled

class ListHabitsBehaviorTest : BaseUnitTest() {
    private val dirFinder: ListHabitsBehavior.DirFinder = mock()

    private val prefs: Preferences = mock()
    private lateinit var behavior: ListHabitsBehavior

    private val screen: ListHabitsBehavior.Screen = mock()
    private lateinit var habit1: Habit
    private lateinit var habit2: Habit

    private var capturedPicker: NumberPickerCallback? = null

    private val bugReporter: ListHabitsBehavior.BugReporter = mock()

    @BeforeTest
    override fun setUp() {
        super.setUp()
        habit1 = fixtures.createShortHabit()
        habit2 = fixtures.createNumericalHabit()
        habitList.add(habit1)
        habitList.add(habit2)
        habitList = spy(habitList)
        behavior = ListHabitsBehavior(
            habitList,
            dirFinder,
            taskRunner,
            screen,
            commandRunner,
            prefs,
            bugReporter
        )
    }

    @Test
    fun testOnEdit() {
        val today = getToday()
        every {
            screen.showNumberPopup(any(), any(), any())
        } calls { args ->
            capturedPicker = args.arg<NumberPickerCallback>(2)
            Unit
        }
        behavior.onEdit(habit2.id!!, today, 0f, 0f)
        verify {
            screen.showNumberPopup(0.1, "", any())
        }
        capturedPicker!!.onNumberPicked(100.0, "")
        assertEquals(100000, habit2.computedEntries.get(today).value)
    }

    @Test
    fun testOnExportCSV() = runTest {
        val outputDir = createTempDir()
        every { dirFinder.getCSVOutputDir() } returns outputDir
        behavior.onExportCSV()
        taskRunner.await()
        verify { screen.showSendFileScreen(any()) }
        val files = outputDir.listFiles()
        assertEquals(1, files!!.size)
    }

    @Test
    fun testOnExportCSV_fail() = runTest {
        val mockDir: UserFile = mock()
        every { mockDir.resolve(any()) } throws RuntimeException("not writable")
        every { dirFinder.getCSVOutputDir() } returns mockDir
        behavior.onExportCSV()
        taskRunner.await()
        verify { screen.showMessage(ListHabitsBehavior.Message.COULD_NOT_EXPORT) }
    }

    @Test
    fun testOnHabitClick() {
        behavior.onClickHabit(habit1.id!!)
        verify { screen.showHabitScreen(habit1.id!!) }
    }

    @Test
    fun testReorderHabit() {
        habitList.primaryOrder = HabitList.Order.BY_POSITION
        behavior.reorderHabit(habit2.id!!, habit1.id!!, true, "")
        assertEquals(habit2, habitList.getByPosition(0))
        assertEquals(habit1, habitList.getByPosition(1))
    }

    @Test
    fun testReorderHabit_crossCategory() {
        habitList.primaryOrder = HabitList.Order.BY_POSITION
        habit1.category = "A"
        habit1.position = 0
        habit2.category = "B"
        habit2.position = 1
        habitList.update(listOf(habit1, habit2))
        behavior.reorderHabit(habit2.id!!, habit1.id!!, true, "A")
        assertEquals("A", habit2.category)
        assertEquals(habit2, habitList.getByPosition(0))
        assertEquals(habit1, habitList.getByPosition(1))
    }

    @Test
    fun testReorderHabit_refIdNull_categoryOnly() {
        habitList.primaryOrder = HabitList.Order.BY_POSITION
        habit1.category = "A"
        habit1.position = 0
        habit2.category = "B"
        habit2.position = 1
        habitList.update(listOf(habit1, habit2))
        behavior.reorderHabit(habit2.id!!, null, false, "A")
        assertEquals("A", habit2.category)
        assertEquals(habit1, habitList.getByPosition(0))
        assertEquals(habit2, habitList.getByPosition(1))
    }

    @Test
    fun testOnRenameCategory() {
        habit1.category = "Old"
        var callback: ((String) -> Unit)? = null
        every {
            screen.showCategoryInput(any(), any())
        } calls { args ->
            assertEquals("Old", args.arg<String>(0))
            callback = args.arg<(String) -> Unit>(1)
            Unit
        }
        behavior.onRenameCategory("Old")
        callback!!.invoke("New")
        assertEquals("New", habit1.category)
    }

    @Test
    fun testOnRepairDB() {
        behavior.onRepairDB()
        verify { habitList.repair() }
        verify { screen.showMessage(ListHabitsBehavior.Message.DATABASE_REPAIRED) }
    }

    @Test
    fun testOnSendBugReport() {
        every { bugReporter.getBugReport() } returns "hello"
        behavior.onSendBugReport()
        verify { bugReporter.dumpBugReportToFile() }
        verify { screen.showSendBugReportToDeveloperScreen("hello") }
        every { bugReporter.getBugReport() } throws RuntimeException()
        behavior.onSendBugReport()
        verify { screen.showMessage(ListHabitsBehavior.Message.COULD_NOT_GENERATE_BUG_REPORT) }
    }

    @Test
    fun testOnStartup_firstLaunch() {
        val today = getToday()
        every { prefs.isFirstRun } returns true
        behavior.onStartup()
        verify { prefs.isFirstRun = false }
        verify { prefs.updateLastHint(-1, today) }
        verify { screen.showIntroScreen() }
    }

    @Test
    fun testOnStartup_notFirstLaunch() {
        every { prefs.isFirstRun } returns false
        behavior.onStartup()
        verify { prefs.incrementLaunchCount() }
    }

    @Test
    fun testOnToggle() {
        assertTrue(habit1.isCompletedToday())
        behavior.onToggle(
            habitId = habit1.id!!,
            date = getToday(),
            value = Entry.NO,
            notes = "",
            x = 0f,
            y = 0f
        )
        assertFalse(habit1.isCompletedToday())
    }

    private fun stubNumberPopup(): (() -> NumberPickerCallback) {
        var picker: NumberPickerCallback? = null
        every {
            screen.showNumberPopup(any(), any(), any())
        } calls { args ->
            picker = args.arg<NumberPickerCallback>(2)
            Unit
        }
        return { picker!! }
    }

    private fun createNumericalHabit(
        frequency: Frequency,
        targetType: NumericalHabitType,
        targetValue: Double,
        color: PaletteColor
    ): Habit {
        val h = modelFactory.buildHabit().apply {
            name = "Test Numerical"
            type = HabitType.NUMERICAL
            this.frequency = frequency
            this.targetType = targetType
            this.targetValue = targetValue
            this.color = color
        }
        habitList.add(h)
        return h
    }

    @Test
    fun testOnEdit_numericalDailyConfetti() {
        val today = LocalDate(2026, 5, 10)
        setToday(today)
        try {
            val getPicker = stubNumberPopup()
            val atLeast = createNumericalHabit(
                Frequency.DAILY,
                NumericalHabitType.AT_LEAST,
                10.0,
                PaletteColor(1)
            )

            behavior.onEdit(atLeast.id!!, today, 1f, 2f)
            getPicker().onNumberPicked(5.0, "")
            verify(notCalled) { screen.showConfetti(atLeast.color, any(), any()) }

            behavior.onEdit(atLeast.id!!, today, 1f, 2f)
            getPicker().onNumberPicked(Entry.SKIP.toDouble() / 1000, "")
            verify(notCalled) { screen.showConfetti(atLeast.color, any(), any()) }

            behavior.onEdit(atLeast.id!!, today, 1f, 2f)
            getPicker().onNumberPicked(10.0, "")
            verify(exactly(1)) { screen.showConfetti(atLeast.color, 1f, 2f) }

            val atMost = createNumericalHabit(
                Frequency.DAILY,
                NumericalHabitType.AT_MOST,
                5.0,
                PaletteColor(2)
            )
            behavior.onEdit(atMost.id!!, today, 3f, 4f)
            getPicker().onNumberPicked(7.0, "")
            verify(notCalled) { screen.showConfetti(atMost.color, any(), any()) }

            behavior.onEdit(atMost.id!!, today, 3f, 4f)
            getPicker().onNumberPicked(4.0, "")
            verify(exactly(1)) { screen.showConfetti(atMost.color, 3f, 4f) }
        } finally {
            resetToday()
        }
    }

    @Test
    fun testOnEdit_numericalWeeklyConfetti() {
        val sun = LocalDate(2026, 5, 10)
        val mon = sun.plus(1)
        val tue = sun.plus(2)
        val wed = sun.plus(3)
        setFirstWeekday(DayOfWeek.SUNDAY)
        setToday(wed)
        try {
            val getPicker = stubNumberPopup()
            val weeklyAtLeast = createNumericalHabit(
                Frequency.WEEKLY,
                NumericalHabitType.AT_LEAST,
                70.0,
                PaletteColor(3)
            )

            behavior.onEdit(weeklyAtLeast.id!!, sun, 1f, 1f)
            getPicker().onNumberPicked(30.0, "")
            verify(notCalled) { screen.showConfetti(weeklyAtLeast.color, any(), any()) }

            behavior.onEdit(weeklyAtLeast.id!!, mon, 1f, 1f)
            getPicker().onNumberPicked(20.0, "")
            verify(notCalled) { screen.showConfetti(weeklyAtLeast.color, any(), any()) }

            behavior.onEdit(weeklyAtLeast.id!!, tue, 1f, 1f)
            getPicker().onNumberPicked(20.0, "")
            verify(exactly(1)) { screen.showConfetti(weeklyAtLeast.color, 1f, 1f) }

            // Saving 0 on another day in an already-completed week should not re-trigger confetti
            behavior.onEdit(weeklyAtLeast.id!!, wed, 1f, 1f)
            getPicker().onNumberPicked(0.0, "")
            verify(notCalled) { screen.showConfetti(weeklyAtLeast.color, any(), any()) }

            val weeklyAtMost = createNumericalHabit(
                Frequency.WEEKLY,
                NumericalHabitType.AT_MOST,
                10.0,
                PaletteColor(4)
            )
            behavior.onEdit(weeklyAtMost.id!!, sun, 2f, 2f)
            getPicker().onNumberPicked(6.0, "")
            verify(exactly(1)) { screen.showConfetti(weeklyAtMost.color, 2f, 2f) }

            behavior.onEdit(weeklyAtMost.id!!, mon, 2f, 2f)
            getPicker().onNumberPicked(5.0, "")
            verify(notCalled) { screen.showConfetti(weeklyAtMost.color, any(), any()) }
        } finally {
            resetToday()
            resetFirstWeekday()
        }
    }

    @Test
    fun testOnEdit_numericalMonthlyHijriConfetti() {
        val m1 = LocalDate.fromHijri(1448, 6, 1)
        val m5 = LocalDate.fromHijri(1448, 6, 5)
        setToday(m5)
        try {
            val getPicker = stubNumberPopup()
            val monthly = createNumericalHabit(
                Frequency(1, 30),
                NumericalHabitType.AT_LEAST,
                100.0,
                PaletteColor(5)
            )

            behavior.onEdit(monthly.id!!, m1, 5f, 6f)
            getPicker().onNumberPicked(40.0, "")
            verify(notCalled) { screen.showConfetti(monthly.color, any(), any()) }

            behavior.onEdit(monthly.id!!, m5, 5f, 6f)
            getPicker().onNumberPicked(60.0, "")
            verify(exactly(1)) { screen.showConfetti(monthly.color, 5f, 6f) }
        } finally {
            resetToday()
        }
    }
}
