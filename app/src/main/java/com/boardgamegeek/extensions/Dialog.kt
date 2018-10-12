package com.boardgamegeek.extensions

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

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
