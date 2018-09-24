package com.boardgamegeek.extensions

import android.widget.EditText

fun EditText.setAndSelectExistingText(existingText: String?) {
    setText(existingText)
    setSelection(0, existingText?.length ?: 0)
}
