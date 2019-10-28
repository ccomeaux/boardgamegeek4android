package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.boardgamegeek.ui.BuddyActivity

class BuddyPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        intent = BuddyActivity.createIntent(context, summary.toString(), null)
    }
}
