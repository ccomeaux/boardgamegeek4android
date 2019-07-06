package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.boardgamegeek.extensions.versionName

class VersionPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    override fun getSummary(): CharSequence {
        return context.versionName()
    }

    override fun isSelectable(): Boolean {
        return false
    }
}
