package com.boardgamegeek.pref

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.boardgamegeek.R

class SyncTimestampsDialogPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
    init {
        setDialogTitle(R.string.pref_sync_timestamps_title)
        dialogLayoutResource = R.layout.dialog_sync_stats
        setPositiveButtonText(R.string.close)
        negativeButtonText = ""
    }
}
