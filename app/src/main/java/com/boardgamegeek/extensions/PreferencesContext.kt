package com.boardgamegeek.extensions

import android.content.Context
import android.preference.PreferenceManager
import com.boardgamegeek.R
import java.util.*

fun Context?.isStatusSetToSync(status: String): Boolean {
    if (this == null) return false
    return getSyncStatuses()?.contains(status) ?: false
}

fun Context.getSyncStatuses(): Set<String>? {
    return getSyncStatuses(resources.getStringArray(R.array.pref_sync_status_default))
}

fun Context.getSyncStatuses(defValues: Array<String>?): Set<String>? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    val defSet = if (defValues == null) null else HashSet(Arrays.asList(*defValues))
    return sharedPreferences.getStringSet(KEY_SYNC_STATUSES, defSet)
}

const val KEY_SYNC_STATUSES = "sync_statuses"
const val COLLECTION_STATUS_RATED = "rated"


