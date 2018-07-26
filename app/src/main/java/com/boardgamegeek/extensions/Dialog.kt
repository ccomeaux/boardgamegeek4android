package com.boardgamegeek.extensions

import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager

fun FragmentActivity.showAndSurvive(dialog: DialogFragment) {
    val fragmentManager = supportFragmentManager
    showAndSurvive(dialog, fragmentManager)
}

fun Fragment.showAndSurvive(dialog: DialogFragment) {
    val fragmentManager = fragmentManager
    showAndSurvive(dialog, fragmentManager)
}

private fun showAndSurvive(dialog: DialogFragment, fragmentManager: FragmentManager?) {
    if (fragmentManager == null) return
    val tag = "dialog"

    val ft = fragmentManager.beginTransaction()
    val prev = fragmentManager.findFragmentByTag(tag)
    if (prev != null) ft.remove(prev)
    ft.addToBackStack(null)

    dialog.show(ft, tag)
}
