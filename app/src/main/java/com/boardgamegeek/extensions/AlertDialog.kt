package com.boardgamegeek.extensions

import android.support.v7.app.AlertDialog
import android.view.View
import android.view.WindowManager

fun AlertDialog.requestFocus(view: View?) {
    view?.requestFocus()
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
}