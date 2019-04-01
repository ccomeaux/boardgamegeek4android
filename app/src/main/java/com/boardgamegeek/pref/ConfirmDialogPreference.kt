package com.boardgamegeek.pref

import android.content.Context

import androidx.preference.DialogPreference

import android.util.AttributeSet

import com.boardgamegeek.R

class ConfirmDialogPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
    init {
        dialogTitle = "$dialogTitle?"
        dialogLayoutResource = R.layout.widget_dialogpreference_textview
    }
}
