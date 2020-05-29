@file:JvmName("PreferenceUtils")

package com.boardgamegeek.extensions

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.core.content.edit
import com.boardgamegeek.R
import com.boardgamegeek.model.Player
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

object CollectionView {
    const val PREFERENCES_KEY_DEFAULT_ID = "viewDefaultId"
    const val DEFAULT_DEFAULT_ID: Long = -1L
}

//region SYNC

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

const val KEY_SYNC_UPLOADS = "sync_uploads"
const val KEY_SYNC_NOTIFICATIONS = "sync_notifications"
const val KEY_SYNC_ERRORS = "sync_errors"
const val KEY_SYNC_ONLY_WIFI = "sync_only_wifi"
const val KEY_SYNC_ONLY_CHARGING = "sync_only_charging"

fun SharedPreferences.getSyncShowNotifications(): Boolean {
    return this[KEY_SYNC_NOTIFICATIONS, false] ?: false
}

fun SharedPreferences.getSyncShowErrors(): Boolean {
    return this[KEY_SYNC_ERRORS, false] ?: false
}

fun SharedPreferences.getSyncOnlyCharging(): Boolean {
    return this[KEY_SYNC_ONLY_CHARGING, false] ?: false
}

fun SharedPreferences.getSyncOnlyWifi(): Boolean {
    return this[KEY_SYNC_ONLY_WIFI, false] ?: false
}

private const val KEY_SYNC_STATUSES_OLD = "syncStatuses"
private const val SEPARATOR = "OV=I=XseparatorX=I=VO"

fun SharedPreferences.getOldSyncStatuses(context: Context): Array<String?> {
    val value = this[KEY_SYNC_STATUSES_OLD, ""] ?: ""
    return if (value.isEmpty()) context.resources.getStringArray(R.array.pref_sync_status_default)
    else value.split(SEPARATOR).toTypedArray()
}


// endregion SYNC

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

// region PLAY STATS

object PlayStats {
    private const val LOG_PLAY_STATS_PREFIX = "logPlayStats"
    const val LOG_PLAY_STATS_INCOMPLETE = LOG_PLAY_STATS_PREFIX + "Incomplete"
    const val LOG_PLAY_STATS_EXPANSIONS = LOG_PLAY_STATS_PREFIX + "Expansions"
    const val LOG_PLAY_STATS_ACCESSORIES = LOG_PLAY_STATS_PREFIX + "Accessories"

    const val KEY_GAME_H_INDEX = "hIndex"
    const val KEY_PLAYER_H_INDEX = "play_stats_player_h_index"
    const val KEY_H_INDEX_N_SUFFIX = "_n"
}

// endregion PLAY STATS

// region FORUMS

const val KEY_ADVANCED_DATES = "advancedForumDates"

// endregion FORUMS

//region LAST PLAY

private const val KEY_LAST_PLAY_TIME = "last_play_time"
private const val KEY_LAST_PLAY_LOCATION = "last_play_location"
private const val KEY_LAST_PLAY_PLAYERS = "last_play_players"
private const val SEPARATOR_RECORD = "OV=I=XrecordX=I=VO"
private const val SEPARATOR_FIELD = "OV=I=XfieldX=I=VO"

fun SharedPreferences.getLastPlayTime(): Long {
    return this[KEY_LAST_PLAY_TIME, 0L] ?: 0L
}

fun SharedPreferences.putLastPlayTime(millis: Long) {
    this[KEY_LAST_PLAY_TIME] = millis
}

fun SharedPreferences.getLastPlayLocation(): String {
    return this[KEY_LAST_PLAY_LOCATION, ""] ?: ""
}

fun SharedPreferences.putLastPlayLocation(location: String?) {
    this[KEY_LAST_PLAY_LOCATION] = location
}

fun SharedPreferences.getLastPlayPlayers(): List<Player>? {
    val players: MutableList<Player> = ArrayList()
    val playersString = this[KEY_LAST_PLAY_PLAYERS, ""] ?: ""
    val playerStringArray = playersString.split(SEPARATOR_RECORD).toTypedArray()
    for (playerString in playerStringArray) {
        if (!TextUtils.isEmpty(playerString)) {
            val playerSplit = playerString.split(SEPARATOR_FIELD).toTypedArray()
            if (playerSplit.size in 1..2) {
                val player = Player()
                player.name = playerSplit[0]
                if (playerSplit.size == 2) {
                    player.username = playerSplit[1]
                }
                players.add(player)
            }
        }
    }
    return players
}

fun SharedPreferences.putLastPlayPlayers(players: List<Player>) {
    val sb = StringBuilder()
    for (player in players) {
        sb.append(player.name).append(SEPARATOR_FIELD).append(player.username).append(SEPARATOR_RECORD)
    }
    this[KEY_LAST_PLAY_PLAYERS] = sb.toString()
}

//endregion LAST PLAY

private const val KEY_PRIVACY_CHECK_TIMESTAMP = "privacy_check_timestamp"

fun SharedPreferences.getLastPrivacyCheckTimestamp(): Long? {
    return this[KEY_PRIVACY_CHECK_TIMESTAMP, 0L] ?: 0L
}

fun SharedPreferences.setLastPrivacyCheckTimestamp() {
    this[KEY_PRIVACY_CHECK_TIMESTAMP] = System.currentTimeMillis()
}

private fun SharedPreferences.putStringSet(key: String, value: Set<String>) {
    this.edit {
        putStringSet(key, value)
    }
}

const val KEY_LOGIN = "login"
const val KEY_LOGOUT = "logout"

const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_ARTISTS = "statsCalculatedTimestampArtists"
const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_DESIGNERS = "statsCalculatedTimestampDesigners"
const val PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS = "statsCalculatedTimestampPublishers"

const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
const val KEY_HAS_SEEN_NAV_DRAWER = "has_seen_nav_drawer"
