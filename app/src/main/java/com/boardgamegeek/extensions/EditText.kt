package com.boardgamegeek.extensions

import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.core.view.postDelayed
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

fun EditText.setAndSelectExistingText(existingText: String?) {
    setText(existingText)
    selectAll()
}

fun EditText.getInt(defaultValue: Int = 0): Int {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    return if (text.isNullOrBlank()) defaultValue else {
        try {
            numberFormat.parse(text.trim().toString())?.toInt() ?: defaultValue
        } catch (e: ParseException) {
            defaultValue
        }
    }
}

fun EditText.getIntOrNull(): Int? {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    return if (text.isNullOrBlank()) null else {
        try {
            numberFormat.parse(text.trim().toString())?.toInt()
        } catch (e: ParseException) {
            null
        }
    }
}

fun EditText.getDoubleOrNull(): Double? {
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())
    return if (text.isNullOrBlank()) null else {
        try {
            numberFormat.parse(text.trim().toString())?.toDouble()
        } catch (e: ParseException) {
            null
        }
    }
}

fun EditText.requestFocusAndKeyboard() {
    requestFocus()
    this.postDelayed(200) {
        context.getSystemService<InputMethodManager>()?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}
