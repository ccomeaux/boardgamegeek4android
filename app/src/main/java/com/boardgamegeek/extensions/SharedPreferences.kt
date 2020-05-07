@file:JvmName("PreferenceUtils")

package com.boardgamegeek.extensions

import android.content.SharedPreferences
import androidx.core.content.edit
import com.boardgamegeek.provider.BggContract
import java.util.*

/**
 * puts a key value pair in shared prefs if doesn't exists, otherwise updates value on given [key]
 */
operator fun SharedPreferences.set(key: String, value: Any?) {
    when (value) {
        is String? -> edit { putString(key, value) }
        is Int -> edit { putInt(key, value) }
        is Boolean -> edit { putBoolean(key, value) }
        is Float -> edit { putFloat(key, value) }
        is Long -> edit { putLong(key, value) }
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}

/**
 * finds value on given key.
 * [T] is the type of value
 * @param defaultValue optional default value - will take null for strings, false for bool and -1 for numeric values if [defaultValue] is not specified
 */
inline operator fun <reified T : Any> SharedPreferences.get(key: String, defaultValue: T? = null): T? {
    return when (T::class) {
        String::class -> getString(key, defaultValue as? String) as T?
        Int::class -> getInt(key, defaultValue as? Int ?: -1) as T?
        Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T?
        Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T?
        Long::class -> getLong(key, defaultValue as? Long ?: -1) as T?
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}

fun SharedPreferences.remove(key: String) {
    edit { remove(key) }
}

private const val VIEW_DEFAULT_ID = "viewDefaultId"
const val VIEW_ID_COLLECTION: Long = -1L

fun SharedPreferences.getViewDefaultId(): Long {
    return this[VIEW_DEFAULT_ID, VIEW_ID_COLLECTION] ?: VIEW_ID_COLLECTION
}

fun SharedPreferences.putViewDefaultId(id: Long) {
    this[VIEW_DEFAULT_ID] = id
}

fun SharedPreferences.removeViewDefaultId() {
    this.remove(VIEW_DEFAULT_ID)
}

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

fun SharedPreferences.isCollectionSetToSync(): Boolean {
    return this.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null).orEmpty().isNotEmpty()
}

fun SharedPreferences.addSyncStatus(status: String) {
    if (status.isBlank()) return
    if (this.isStatusSetToSync(status)) return
    val statuses: MutableSet<String> = this.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null).orEmpty().toMutableSet()
    statuses.add(status)
    this.putStringSet(PREFERENCES_KEY_SYNC_STATUSES, statuses)
}

fun SharedPreferences.setSyncStatuses(statuses: Array<String>) {
    this.putStringSet(PREFERENCES_KEY_SYNC_STATUSES, HashSet(listOf(*statuses)))
}

fun SharedPreferences.isStatusSetToSync(status: String): Boolean {
    return this.getSyncStatusesOrDefault().contains(status)
}

fun SharedPreferences.getSyncStatusesOrDefault(): Set<String> {
    return this.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, setOf(COLLECTION_STATUS_OWN)).orEmpty()
}

fun SharedPreferences.getSyncStatusesAsSql(): String {
    val selection = StringBuilder()
    val statuses = this.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null) ?: emptySet()
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

// region PLAY LOGGING

private const val LOG_EDIT_PLAYER_PROMPTED = "logEditPlayerPrompted"
private const val LOG_EDIT_PLAYER = "logEditPlayer"

fun SharedPreferences.getEditPlayerPrompted(): Boolean {
    return this[LOG_EDIT_PLAYER_PROMPTED, false] ?: false
}

fun SharedPreferences.putEditPlayerPrompted() {
    this[LOG_EDIT_PLAYER_PROMPTED] = true
}

fun SharedPreferences.getEditPlayer(): Boolean {
    return this[LOG_EDIT_PLAYER, false] ?: false
}

fun SharedPreferences.putEditPlayer(value: Boolean) {
    this[LOG_EDIT_PLAYER] = value
}

fun SharedPreferences.showLogPlay(): Boolean {
    return showLogPlayField("logPlay", "logHideLog", true)
}

fun SharedPreferences.showQuickLogPlay(): Boolean {
    return showLogPlayField("quickLogPlay", "logHideQuickLog", true)
}

fun SharedPreferences.showLogPlayLocation(): Boolean {
    return showLogPlayField("logPlayLocation", "logHideLocation")
}

fun SharedPreferences.showLogPlayLength(): Boolean {
    return showLogPlayField("logPlayLength", "logHideLength")
}

fun SharedPreferences.showLogPlayIncomplete(): Boolean {
    return showLogPlayField("logPlayIncomplete", "logHideIncomplete")
}

fun SharedPreferences.showLogPlayNoWinStats(): Boolean {
    return showLogPlayField("logPlayNoWinStats", "logHideNoWinStats")
}

fun SharedPreferences.showLogPlayComments(): Boolean {
    return showLogPlayField("logPlayComments", "logHideComments")
}

fun SharedPreferences.showLogPlayQuantity(): Boolean {
    return this["logPlayQuantity", false] ?: false
}

fun SharedPreferences.showLogPlayPlayerList(): Boolean {
    return showLogPlayField("logPlayPlayerList", "logHidePlayerList", true)
}

fun SharedPreferences.showLogPlayerTeamColor(): Boolean {
    return showLogPlayField("logPlayerTeamColor", "logHideTeamColor")
}

fun SharedPreferences.showLogPlayerPosition(): Boolean {
    return showLogPlayField("logPlayerPosition", "logHidePosition")
}

fun SharedPreferences.showLogPlayerScore(): Boolean {
    return showLogPlayField("logPlayerScore", "logHideScore")
}

fun SharedPreferences.showLogPlayerRating(): Boolean {
    return showLogPlayField("logPlayerRating", "logHideRating")
}

fun SharedPreferences.showLogPlayerNew(): Boolean {
    return showLogPlayField("logPlayerNew", "logHideNew")
}

fun SharedPreferences.showLogPlayerWin(): Boolean {
    return showLogPlayField("logPlayerWin", "logHideWin")
}

private fun SharedPreferences.showLogPlayField(key: String, oldKey: String, defaultValue: Boolean = false): Boolean {
    return this[key, !(this[oldKey, !defaultValue] ?: !defaultValue)] ?: defaultValue
}

// endregion PLAY LOGGING

private fun SharedPreferences.putStringSet(key: String, value: Set<String>) {
    this.edit {
        putStringSet(key, value)
    }
}

const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS = "statsCalculatedTimestampArtists"
const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS = "statsCalculatedTimestampDesigners"
const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS = "statsCalculatedTimestampPublishers"
