package com.boardgamegeek.extensions

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.boardgamegeek.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

fun FragmentActivity.showAndSurvive(dialog: DialogFragment, tag: String = "dialog") {
    showAndSurvive(dialog, supportFragmentManager, tag)
}

fun Fragment.showAndSurvive(dialog: DialogFragment, tag: String = "dialog") {
    showAndSurvive(dialog, parentFragmentManager, tag)
}

private fun showAndSurvive(dialog: DialogFragment, fragmentManager: FragmentManager, tag: String = "dialog") {
    fragmentManager.beginTransaction().apply {
        fragmentManager.findFragmentByTag(tag)?.let {
            remove(it)
        }
        addToBackStack(null)
        dialog.show(this, tag)
    }
}

fun Context.createConfirmationDialog(
    messageId: Int,
    @StringRes positiveButtonTextId: Int,
    okListener: DialogInterface.OnClickListener?,
): Dialog {
    val builder = createThemedBuilder()
        .setCancelable(true)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(positiveButtonTextId, okListener)
    if (messageId > 0) builder.setMessage(messageId)
    return builder.create()
}

fun Activity.createDiscardDialog(
    @StringRes objectResId: Int,
    @StringRes positiveButtonResId: Int = R.string.keep_editing,
    isNew: Boolean,
    finishActivity: Boolean = true,
    discardListener: () -> Unit = {}
): Dialog {
    val messageFormat = getString(
        if (isNew)
            R.string.discard_new_message
        else
            R.string.discard_changes_message
    )
    return createThemedBuilder()
        .setMessage(String.format(messageFormat, getString(objectResId).lowercase(Locale.getDefault())))
        .setPositiveButton(positiveButtonResId, null)
        .setNegativeButton(R.string.discard) { _, _ ->
            discardListener()
            if (finishActivity) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
        .setCancelable(true)
        .create()
}

fun Context.createThemedBuilder(): AlertDialog.Builder {
    return MaterialAlertDialogBuilder(this, R.style.Theme_bgglight_Dialog_Alert)
}

fun Context.showClickableAlertDialog(@StringRes titleResId: Int, @StringRes messageResId: Int, vararg formatArgs: Any) {
    val spannableMessage = SpannableString(getString(messageResId, *formatArgs))
    showClickableAlertDialog(spannableMessage, titleResId)
}

fun Context.showClickableAlertDialogPlural(@StringRes titleResId: Int, @PluralsRes messageResId: Int, quantity: Int, vararg formatArgs: Any) {
    val spannableMessage = SpannableString(resources.getQuantityString(messageResId, quantity, *formatArgs))
    showClickableAlertDialog(spannableMessage, titleResId)
}

private fun Context.showClickableAlertDialog(spannableMessage: SpannableString, titleResId: Int) {
    Linkify.addLinks(spannableMessage, Linkify.WEB_URLS)
    val dialog = AlertDialog.Builder(this)
        .setTitle(titleResId)
        .setMessage(spannableMessage)
        .show()
    dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
}
