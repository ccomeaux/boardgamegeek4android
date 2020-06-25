package com.boardgamegeek.extensions

import android.widget.EditText
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

fun EditText.setAndSelectExistingText(existingText: String?) {
    setText(existingText)
    setSelection(0, existingText?.length ?: 0)
}

fun EditText.getInt(defaultValue: Int = 0): Int {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    return if (text.isNullOrBlank()) defaultValue else {
        try {
            numberFormat.parse(text.trim().toString()).toInt()
        } catch (e: ParseException) {
            defaultValue
        }
    }
}

fun EditText.getIntOrNull(): Int? {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    return if (text.isNullOrBlank()) null else {
        try {
            numberFormat.parse(text.trim().toString()).toInt()
        } catch (e: ParseException) {
            null
        }
    }
}

fun EditText.getDouble(defaultValue: Double = 0.0): Double {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    return if (text.isNullOrBlank()) defaultValue else {
        try {
            numberFormat.parse(text.trim().toString()).toDouble()
        } catch (e: ParseException) {
            defaultValue
        }
    }
}

fun EditText.getDoubleOrNull(): Double? {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    return if (text.isNullOrBlank()) null else {
        try {
            numberFormat.parse(text.trim().toString()).toDouble()
        } catch (e: ParseException) {
            null
        }
    }
}
