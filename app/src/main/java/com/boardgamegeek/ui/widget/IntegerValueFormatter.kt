package com.boardgamegeek.ui.widget

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler

import java.text.DecimalFormat

class IntegerValueFormatter(private val shouldSuppressZero: Boolean) : IValueFormatter {
    private val format: DecimalFormat = DecimalFormat("#0")

    override fun getFormattedValue(value: Float, entry: Entry, dataSetIndex: Int, viewPortHandler: ViewPortHandler): String {
        return if (shouldSuppressZero && value == 0.0f) "" else format.format(value.toDouble())
    }
}
