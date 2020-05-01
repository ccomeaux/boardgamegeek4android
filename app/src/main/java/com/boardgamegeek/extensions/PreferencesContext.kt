@file:JvmName("PreferenceUtils")

package com.boardgamegeek.extensions

import android.content.Context
import androidx.core.content.edit
import com.boardgamegeek.PreferenceHelper.get
import com.boardgamegeek.PreferenceHelper.preferences
import com.boardgamegeek.PreferenceHelper.remove
import com.boardgamegeek.PreferenceHelper.set
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.PreferencesUtils
import java.util.*

fun Context.getViewDefaultId(): Long {
    return preferences(this)[VIEW_DEFAULT_ID, PreferencesUtils.VIEW_ID_COLLECTION]
            ?: PreferencesUtils.VIEW_ID_COLLECTION
}

fun Context.putViewDefaultId(id: Long) {
    preferences(this)[VIEW_DEFAULT_ID] = id
}

fun Context.removeViewDefaultId() {
    preferences(this).remove(VIEW_DEFAULT_ID)
}

fun Context?.addSyncStatus(status: String) {
    if (this == null) return
    if (status.isBlank()) return
    if (isStatusSetToSync(status)) return
    val statuses: MutableSet<String> = getSyncStatuses(null)?.toMutableSet() ?: mutableSetOf()
    statuses.add(status)
    putStringSet(PREFERENCES_KEY_SYNC_STATUSES, statuses)
}

fun Context?.setSyncStatuses(statuses: Array<String>) {
    this?.putStringSet(PREFERENCES_KEY_SYNC_STATUSES, HashSet(listOf(*statuses)))
}

fun Context?.isStatusSetToSync(status: String): Boolean {
    return this?.getSyncStatuses()?.contains(status) ?: false
}

fun Context?.getSyncStatuses(): Set<String>? {
    return getSyncStatuses(this?.resources?.getStringArray(R.array.pref_sync_status_default))
}

fun Context?.getSyncStatuses(defValues: Array<String>? = null): Set<String>? {
    return if (this == null)
        null
    else
        preferences(this).getStringSet(PREFERENCES_KEY_SYNC_STATUSES, defValues?.toSet())
}

fun Context?.getSyncStatusesAsSql(): String {
    val selection = StringBuilder()
    val statuses = getSyncStatuses() ?: emptySet()
    for (status in statuses) {
        if (status.isBlank()) continue
        if (selection.isNotBlank()) selection.append(" OR ")
        selection.append(when (status) {
            COLLECTION_STATUS_OWN -> BggContract.Collection.STATUS_OWN.isTrue()
            COLLECTION_STATUS_PREVIOUSLY_OWNED -> BggContract.Collection.STATUS_PREVIOUSLY_OWNED.isTrue()
            COLLECTION_STATUS_PREORDERED -> BggContract.Collection.STATUS_PREORDERED.isTrue()
            COLLECTION_STATUS_FOR_TRADE -> BggContract.Collection.STATUS_FOR_TRADE.isTrue()
            COLLECTION_STATUS_WANT -> BggContract.Collection.STATUS_WANT.isTrue()
            COLLECTION_STATUS_WANT_TO_BUY -> BggContract.Collection.STATUS_WANT_TO_BUY.isTrue()
            COLLECTION_STATUS_WANT_TO_PLAY -> BggContract.Collection.STATUS_WANT_TO_PLAY.isTrue()
            COLLECTION_STATUS_WISHLIST -> BggContract.Collection.STATUS_WISHLIST.isTrue()
            COLLECTION_STATUS_RATED -> BggContract.Collection.RATING.greaterThanZero()
            COLLECTION_STATUS_PLAYED -> BggContract.Collection.NUM_PLAYS.greaterThanZero()
            COLLECTION_STATUS_COMMENTED -> BggContract.Collection.COMMENT.notBlank()
            COLLECTION_STATUS_HAS_PARTS -> BggContract.Collection.HASPARTS_LIST.notBlank()
            COLLECTION_STATUS_WANT_PARTS -> BggContract.Collection.WANTPARTS_LIST.notBlank()
            else -> ""
        })
    }
    return selection.toString()
}

fun Context?.isCollectionSetToSync(): Boolean {
    val statuses = getSyncStatuses()
    return statuses != null && statuses.isNotEmpty()
}

fun Context.getSyncPlays(): Boolean {
    return preferences(this)[PREFERENCES_KEY_SYNC_PLAYS, false] ?: false
}

fun Context.setSyncPlays() {
    preferences(this)[PREFERENCES_KEY_SYNC_PLAYS] = true
}

fun Context.setSyncPlaysTimestamp() {
    preferences(this)[PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP] = System.currentTimeMillis()
}

fun Context.getSyncBuddies(): Boolean {
    return preferences(this)[PREFERENCES_KEY_SYNC_BUDDIES, false] ?: false
}

fun Context.setSyncBuddies() {
    preferences(this)[PREFERENCES_KEY_SYNC_BUDDIES] = true
}

fun Context.setStatsCalculatedTimestampArtists() {
    preferences(this)[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS] = System.currentTimeMillis()
}

fun Context.setStatsCalculatedTimestampDesigners() {
    preferences(this)[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS] = System.currentTimeMillis()
}

fun Context.setStatsCalculatedTimestampPublishers() {
    preferences(this)[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS] = System.currentTimeMillis()
}

fun Context.getStatsCalculatedTimestampArtists(): Long {
    return preferences(this)[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS, 0L] ?: 0L
}

fun Context.getStatsCalculatedTimestampDesigners(): Long {
    return preferences(this)[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS, 0L] ?: 0L
}

fun Context.getStatsCalculatedTimestampPublishers(): Long {
    return preferences(this)[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS, 0L] ?: 0L
}

private fun Context.putStringSet(key: String, value: Set<String>) {
    preferences(this).edit {
        putStringSet(key, value)
    }
}

private const val VIEW_DEFAULT_ID = "viewDefaultId"

const val PREFERENCES_KEY_SYNC_STATUSES = "sync_statuses"
const val PREFERENCES_KEY_SYNC_PLAYS = "syncPlays"
const val PREFERENCES_KEY_SYNC_PLAYS_TIMESTAMP = "syncPlaysTimestamp"
const val PREFERENCES_KEY_SYNC_BUDDIES = "syncBuddies"

const val COLLECTION_STATUS_OWN = "own"
const val COLLECTION_STATUS_PREVIOUSLY_OWNED = "prevowned"
const val COLLECTION_STATUS_PREORDERED = "preordered"
const val COLLECTION_STATUS_FOR_TRADE = "trade"
const val COLLECTION_STATUS_WANT = "want"
const val COLLECTION_STATUS_WANT_TO_BUY = "wanttobuy"
const val COLLECTION_STATUS_WANT_TO_PLAY = "wanttoplay"
const val COLLECTION_STATUS_WISHLIST = "wishlist"
const val COLLECTION_STATUS_PLAYED = "played"
const val COLLECTION_STATUS_RATED = "rated"
const val COLLECTION_STATUS_COMMENTED = "comment"
const val COLLECTION_STATUS_HAS_PARTS = "hasparts"
const val COLLECTION_STATUS_WANT_PARTS = "wantparts"

private const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS = "statsCalculatedTimestampArtists"
private const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS = "statsCalculatedTimestampDesigners"
private const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS = "statsCalculatedTimestampPublishers"
