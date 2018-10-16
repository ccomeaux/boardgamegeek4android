package com.boardgamegeek.extensions

import android.view.Menu
import android.widget.TextView
import androidx.annotation.IdRes

fun Menu.setActionBarCount(@IdRes id: Int, count: Int) {
    setActionBarText(id, if (count <= 0) "" else "%,d".format(count), null)
}

fun Menu.setActionBarText(@IdRes id: Int, text: String) {
    setActionBarText(id, text, null)
}

fun Menu.setActionBarText(@IdRes id: Int, text1: String, text2: String?) {
    val actionView = findItem(id).actionView ?: return
    actionView.findViewById<TextView>(android.R.id.text1)?.text = text1
    actionView.findViewById<TextView>(android.R.id.text2)?.text = text2
}
