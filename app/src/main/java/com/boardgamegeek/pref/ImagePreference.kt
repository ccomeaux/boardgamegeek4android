package com.boardgamegeek.pref

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.net.toUri
import androidx.preference.Preference
import com.boardgamegeek.R

class ImagePreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    init {
        isEnabled = true
        isSelectable = true
        layoutResource = R.layout.preference_image_layout
        intent = Intent(Intent.ACTION_VIEW, "https://www.boardgamegeek.com/".toUri())
    }
}
