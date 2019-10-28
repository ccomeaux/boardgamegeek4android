package com.boardgamegeek.pref

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.preference.Preference
import android.util.AttributeSet

class LinkPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(summary.toString()))
    }
}
