package com.boardgamegeek.ui.widget

import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

class IntegerValueFormatter(private val shouldSuppressZero: Boolean) : ValueFormatter() {
    private val format: DecimalFormat = DecimalFormat("#0")

    override fun getFormattedValue(value: Float): String {
        return if (shouldSuppressZero && value == 0.0f) "" else format.format(value.toDouble())
    }
}
