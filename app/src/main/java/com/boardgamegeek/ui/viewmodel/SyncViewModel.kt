package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.pref.SyncPrefs

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    val currentSyncTimestamp: LiveSharedPreference<Long> = LiveSharedPreference(getApplication(), SyncPrefs.TIMESTAMP_CURRENT, SyncPrefs.NAME)
}