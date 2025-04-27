package com.boardgamegeek.pref

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.net.toUri
import androidx.preference.Preference

class ChangeLogPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        val changeLogUrl = "https://github.com/ccomeaux/boardgamegeek4android/blob/master/CHANGELOG.md"
        intent = Intent(Intent.ACTION_VIEW, changeLogUrl.toUri())
    }
}
