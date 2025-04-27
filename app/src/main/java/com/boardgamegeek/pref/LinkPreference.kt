package com.boardgamegeek.pref

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.net.toUri
import androidx.preference.Preference

class LinkPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        intent = Intent(Intent.ACTION_VIEW, summary.toString().toUri())
    }
}
