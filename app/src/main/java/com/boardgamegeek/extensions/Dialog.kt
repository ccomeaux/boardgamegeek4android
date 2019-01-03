@file:JvmName("DialogUtils")

package com.boardgamegeek.extensions

import android.app.Activity
import android.app.Dialog
import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.boardgamegeek.R

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

fun FragmentActivity.showFragment(fragment: DialogFragment, tag: String) {
    val ft = supportFragmentManager.beginTransaction()
    val prev = supportFragmentManager.findFragmentByTag(tag)
    if (prev != null) ft.remove(prev)
    ft.addToBackStack(null)
    fragment.show(ft, tag)
}

interface OnDiscardListener {
    fun onDiscard()
}

@JvmOverloads
fun createDiscardDialog(
        activity: Activity,
        @StringRes objectResId: Int,
        isNew: Boolean, finishActivity: Boolean = true,
        @StringRes positiveButtonResId: Int = R.string.keep_editing,
        discardListener: OnDiscardListener? = null): Dialog {
    val messageFormat = activity.getString(if (isNew)
        R.string.discard_new_message
    else
        R.string.discard_changes_message)
    return createThemedBuilder(activity)
            .setMessage(String.format(messageFormat, activity.getString(objectResId).toLowerCase()))
            .setPositiveButton(positiveButtonResId, null)
            .setNegativeButton(R.string.discard) { _, _ ->
                discardListener?.onDiscard()
                if (finishActivity) {
                    activity.setResult(Activity.RESULT_CANCELED)
                    activity.finish()
                }
            }
            .setCancelable(true)
            .create()
}

fun createThemedBuilder(context: Context): AlertDialog.Builder {
    return AlertDialog.Builder(context, R.style.Theme_bgglight_Dialog_Alert)
}
