package com.boardgamegeek.extensions

import android.widget.EditText

fun EditText.setAndSelectExistingText(existingText: String?) {
    setText(existingText)
    setSelection(0, existingText?.length ?: 0)
}

fun EditText.getInt(defaultValue: Int = 0): Int {
    return if (text.isNullOrBlank()) defaultValue else text.trim().toString().toInt()
}

fun EditText.getDouble(defaultValue: Double = 0.0): Double {
    return if (text.isNullOrBlank()) defaultValue else text.trim().toString().toDouble()
}
