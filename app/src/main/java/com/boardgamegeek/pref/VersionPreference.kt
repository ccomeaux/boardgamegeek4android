package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.boardgamegeek.util.HelpUtils

class VersionPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    override fun getSummary(): CharSequence {
        return HelpUtils.getVersionName(context)
    }

    override fun isSelectable(): Boolean {
        return false
    }
}
