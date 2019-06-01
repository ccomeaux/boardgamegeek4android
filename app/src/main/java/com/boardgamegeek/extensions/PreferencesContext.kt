package com.boardgamegeek.extensions

import android.content.Context
import androidx.preference.PreferenceManager
import com.boardgamegeek.R
import java.util.*

fun Context?.addSyncStatus(status: String): Boolean {
    if (this == null) return false
    if (status.isBlank()) return false
    if (isStatusSetToSync(status)) return false
    val statuses: MutableSet<String> = getSyncStatuses(null)?.toMutableSet() ?: mutableSetOf()
    statuses.add(status)
    return putStringSet(KEY_SYNC_STATUSES, statuses)
}

fun Context?.isStatusSetToSync(status: String): Boolean {
    if (this == null) return false
    return getSyncStatuses()?.contains(status) ?: false
}

fun Context.getSyncStatuses(): Set<String>? {
    return getSyncStatuses(resources.getStringArray(R.array.pref_sync_status_default))
}

fun Context.getSyncStatuses(defValues: Array<String>? = null): Set<String>? {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    val defSet = if (defValues == null) null else HashSet(Arrays.asList(*defValues))
    return sharedPreferences.getStringSet(KEY_SYNC_STATUSES, defSet)
}

private fun Context.putStringSet(key: String, value: Set<String>): Boolean {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    val editor = sharedPreferences.edit()
    editor.putStringSet(key, value)
    return editor.commit()
}

const val KEY_SYNC_STATUSES = "sync_statuses"

const val COLLECTION_STATUS_OWN = "own"
const val COLLECTION_STATUS_PLAYED = "played"
const val COLLECTION_STATUS_RATED = "rated"


