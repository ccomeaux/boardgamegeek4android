package com.boardgamegeek.pref

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.preference.Preference
import android.util.AttributeSet

import com.boardgamegeek.BuildConfig

class ChangeLogPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        val changeLogUrl = "https://github.com/ccomeaux/boardgamegeek4android/blob/${BuildConfig.GIT_BRANCH}/CHANGELOG.md"
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(changeLogUrl))
    }
}
