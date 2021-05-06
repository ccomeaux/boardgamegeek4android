package com.boardgamegeek.extensions

import android.view.Menu
import android.widget.TextView
import androidx.annotation.IdRes

fun Menu.setActionBarCount(@IdRes id: Int, count: Int, text: String? = null) {
    setActionBarText(id,
            if (count <= 0) "" else "%,d".format(count),
            if (count <= 0) "" else text)
}

private fun Menu.setActionBarText(@IdRes id: Int, text1: String, text2: String?) {
    val actionView = findItem(id).actionView ?: return
    actionView.findViewById<TextView>(android.R.id.text1)?.text = text1
    if (!text2.isNullOrBlank()) {
        actionView.findViewById<TextView>(android.R.id.text2)?.text = text2
    }
}
