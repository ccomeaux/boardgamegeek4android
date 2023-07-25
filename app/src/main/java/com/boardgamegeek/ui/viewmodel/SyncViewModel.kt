package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.livedata.LiveSharedPreference

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    val username: LiveSharedPreference<String> = LiveSharedPreference(getApplication(), AccountPreferences.KEY_USERNAME)
}
