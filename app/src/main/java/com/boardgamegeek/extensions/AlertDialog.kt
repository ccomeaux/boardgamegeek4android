package com.boardgamegeek.extensions

import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog

fun AlertDialog.requestFocus(view: View? = null) {
    view?.requestFocus()
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
}