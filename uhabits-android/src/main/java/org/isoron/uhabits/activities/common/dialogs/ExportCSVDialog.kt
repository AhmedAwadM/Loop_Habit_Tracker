package org.isoron.uhabits.activities.common.dialogs

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.isoron.platform.time.HIJRI_LONG_MONTH_NAMES
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.platform.time.hijriMonthLength
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.InterfaceUtils.dpToPixels

object ExportCSVDialog {

    fun show(
        context: Context,
        onRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit
    ) {
        val options = arrayOf(
            context.getString(R.string.export_range_week),
            context.getString(R.string.export_range_month),
            context.getString(R.string.export_range_year),
            context.getString(R.string.export_range_custom)
        )
        AlertDialog.Builder(context)
            .setTitle(R.string.export_csv_range_title)
            .setItems(options) { _, which ->
                val today = getToday()
                when (which) {
                    0 -> onRangeSelected(today.minus(6), today)
                    1 -> onRangeSelected(today.minus(29), today)
                    2 -> onRangeSelected(today.minus(today.hijriYearLength - 1), today)
                    3 -> showCustomHijriRangeDialog(context, onRangeSelected)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCustomHijriRangeDialog(
        context: Context,
        onRangeSelected: (startDate: LocalDate, endDate: LocalDate) -> Unit
    ) {
        val today = getToday()
        val defaultStart = today.startOfHijriMonth()
        val pad = dpToPixels(context, 16f).toInt()
        val smallPad = dpToPixels(context, 8f).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, smallPad)
        }

        val fromLabel = TextView(context).apply {
            text = context.getString(R.string.export_from_hijri_date)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, smallPad)
        }
        val (fromRow, getFromDate) = buildHijriDatePickerRow(context, defaultStart)

        val toLabel = TextView(context).apply {
            text = context.getString(R.string.export_to_hijri_date)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, pad, 0, smallPad)
        }
        val (toRow, getToDate) = buildHijriDatePickerRow(context, today)

        container.addView(fromLabel)
        container.addView(fromRow)
        container.addView(toLabel)
        container.addView(toRow)

        val scrollView = ScrollView(context).apply {
            addView(container)
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.export_range_custom)
            .setView(scrollView)
            .setPositiveButton(R.string.export) { _, _ ->
                val fromDate = getFromDate()
                val toDate = getToDate()
                if (fromDate.isOlderThan(toDate)) {
                    onRangeSelected(fromDate, toDate)
                } else {
                    onRangeSelected(toDate, fromDate)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun buildHijriDatePickerRow(
        context: Context,
        initialDate: LocalDate
    ): Pair<LinearLayout, () -> LocalDate> {
        val minYear = (initialDate.hijriYear - 25).coerceAtLeast(1350)
        val maxYear = (initialDate.hijriYear + 10).coerceAtMost(1550)

        val dayPicker = NumberPicker(context).apply {
            minValue = 1
            maxValue = hijriMonthLength(initialDate.hijriYear, initialDate.hijriMonth)
            value = initialDate.hijriDay.coerceIn(1, maxValue)
            wrapSelectorWheel = false
        }

        val monthNames = Array(12) { idx -> "${idx + 1} - ${HIJRI_LONG_MONTH_NAMES[idx]}" }
        val monthPicker = NumberPicker(context).apply {
            minValue = 1
            maxValue = 12
            displayedValues = monthNames
            value = initialDate.hijriMonth.coerceIn(1, 12)
            wrapSelectorWheel = false
        }

        val yearPicker = NumberPicker(context).apply {
            minValue = minYear
            maxValue = maxYear
            value = initialDate.hijriYear.coerceIn(minYear, maxYear)
            wrapSelectorWheel = false
        }

        val updateMaxDay = {
            val maxD = hijriMonthLength(yearPicker.value, monthPicker.value)
            if (dayPicker.value > maxD) {
                dayPicker.value = maxD
            }
            dayPicker.maxValue = maxD
        }
        monthPicker.setOnValueChangedListener { _, _, _ -> updateMaxDay() }
        yearPicker.setOnValueChangedListener { _, _, _ -> updateMaxDay() }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val lpDay = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val lpMonth = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f)
            val lpYear = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
            addView(dayPicker, lpDay)
            addView(monthPicker, lpMonth)
            addView(yearPicker, lpYear)
        }

        val dateProvider = {
            dayPicker.clearFocus()
            monthPicker.clearFocus()
            yearPicker.clearFocus()
            val y = yearPicker.value
            val m = monthPicker.value
            val maxD = hijriMonthLength(y, m)
            val d = dayPicker.value.coerceIn(1, maxD)
            LocalDate.fromHijri(y, m, d)
        }

        return row to dateProvider
    }
}
