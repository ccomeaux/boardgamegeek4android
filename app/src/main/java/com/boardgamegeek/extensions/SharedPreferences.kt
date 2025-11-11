package com.boardgamegeek.extensions

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.map
import com.boardgamegeek.R
import com.boardgamegeek.extensions.PlayStatPrefs.KEY_GAME_H_INDEX
import com.boardgamegeek.extensions.PlayStatPrefs.KEY_PLAYER_H_INDEX
import com.boardgamegeek.livedata.LiveSharedPreference
import com.boardgamegeek.mappers.mapToEnum
import com.boardgamegeek.mappers.mapToPreference
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.model.HIndex
import com.boardgamegeek.model.PlayPlayer
import com.boardgamegeek.model.Player
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

object CollectionViewPrefs {
    const val PREFERENCES_KEY_DEFAULT_ID = "viewDefaultId"
    const val DEFAULT_DEFAULT_ID: Int = -1
}

object AccountPreferences {
    private const val KEY_PREFIX = "account_"
    const val KEY_USERNAME = KEY_PREFIX + "username"
    const val KEY_FULL_NAME = KEY_PREFIX + "full_name"
    const val KEY_AVATAR_URL = KEY_PREFIX + "avatar_url"
}

//region SYNC

const val PREFERENCES_KEY_SYNC_STATUSES = "sync_statuses"
const val PREFERENCES_KEY_SYNC_PLAYS = "syncPlays"
const val PREFERENCES_KEY_SYNC_PLAYS_DISABLED_TIMESTAMP = "syncPlaysTimestamp"
const val PREFERENCES_KEY_SYNC_BUDDIES = "syncBuddies"

const val COLLECTION_STATUS_OWN = "own"
const val COLLECTION_STATUS_PREVIOUSLY_OWNED = "prevowned"
const val COLLECTION_STATUS_PREORDERED = "preordered"
const val COLLECTION_STATUS_FOR_TRADE = "trade"
const val COLLECTION_STATUS_WANT_IN_TRADE = "want"
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

fun SharedPreferences.addSyncStatus(status: CollectionStatus): Boolean {
    if (status == CollectionStatus.Unknown) return false
    if (this.isStatusSetToSync(status)) return false
    val statuses: MutableSet<String> = this.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null).orEmpty().toMutableSet()
    statuses.add(status.mapToPreference())
    this.putStringSet(PREFERENCES_KEY_SYNC_STATUSES, statuses)
    return true
}

fun SharedPreferences.removeSyncStatus(status: CollectionStatus): Boolean {
    if (status == CollectionStatus.Unknown)
    if (!this.isStatusSetToSync(status)) return false
    val statuses: MutableSet<String> = this.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null).orEmpty().toMutableSet()
    val success = statuses.remove(status.mapToPreference())
    if (success)
        this.putStringSet(PREFERENCES_KEY_SYNC_STATUSES, statuses)
    return success
}

fun SharedPreferences.setSyncStatuses(statuses: Array<String>) {
    this.putStringSet(PREFERENCES_KEY_SYNC_STATUSES, HashSet(listOf(*statuses)))
}

fun SharedPreferences.isStatusSetToSync(status: CollectionStatus): Boolean {
    return this.getSyncStatusesOrDefault().contains(status)
}

fun SharedPreferences.getSyncStatuses(): Set<CollectionStatus> {
    return this.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, null)?.map { it.mapToEnum() }?.toSet().orEmpty()
}

fun SharedPreferences.getSyncStatusesOrDefault(): Set<CollectionStatus> {
    return this.getStringSet(PREFERENCES_KEY_SYNC_STATUSES, setOf(CollectionStatus.Own.mapToPreference()))?.map { it.mapToEnum() }?.toSet().orEmpty()
}

fun collectionStatusLiveData(context: Context) = LiveSharedPreference<Set<String>>(context, PREFERENCES_KEY_SYNC_STATUSES).map { set ->
    set?.map { it.mapToEnum() }?.toSet()
}

const val KEY_SYNC_UPLOADS = "sync_uploads"
const val KEY_SYNC_ERRORS = "sync_errors"
const val KEY_SYNC_PROGRESS = "sync_progress"
const val KEY_SYNC_ONLY_WIFI = "sync_only_wifi"
const val KEY_SYNC_ONLY_CHARGING = "sync_only_charging"

private const val KEY_SYNC_STATUSES_OLD = "syncStatuses"

@Suppress("SpellCheckingInspection")
private const val SEPARATOR = "OV=I=XseparatorX=I=VO"

fun SharedPreferences.getOldSyncStatuses(context: Context): Array<String> {
    val value = this[KEY_SYNC_STATUSES_OLD, ""] ?: ""
    return if (value.isEmpty()) context.resources.getStringArray(R.array.pref_sync_status_default)
    else value.split(SEPARATOR).toTypedArray()
}


// endregion SYNC

// region PLAY LOGGING

const val LOG_EDIT_PLAYER_PROMPTED = "logEditPlayerPrompted"
const val LOG_EDIT_PLAYER = "logEditPlayer"
const val LOG_PLAY_TYPE_FORM = "form"
const val LOG_PLAY_TYPE_QUICK = "quick"
const val LOG_PLAY_TYPE_WIZARD = "wizard"

fun SharedPreferences.logPlayPreference(): String {
    return this.getString("logPlayType", null)
        ?: return when {
            showLogPlayField("logPlay", "logHideLog", true) -> LOG_PLAY_TYPE_FORM
            showLogPlayField("quickLogPlay", "logHideQuickLog", true) -> LOG_PLAY_TYPE_QUICK
            else -> LOG_PLAY_TYPE_WIZARD
        }
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

object PlayStatPrefs {
    private const val LOG_PLAY_STATS_PREFIX = "logPlayStats"
    const val LOG_PLAY_STATS_INCOMPLETE = LOG_PLAY_STATS_PREFIX + "Incomplete"
    const val LOG_PLAY_STATS_EXPANSIONS = LOG_PLAY_STATS_PREFIX + "Expansions"
    const val LOG_PLAY_STATS_ACCESSORIES = LOG_PLAY_STATS_PREFIX + "Accessories"

    const val KEY_GAME_H_INDEX = "hIndex"
    const val KEY_PLAYER_H_INDEX = "play_stats_player_h_index"
    const val KEY_H_INDEX_N_SUFFIX = "_n"
}

enum class HIndexType(val key: String) {
    Game(KEY_GAME_H_INDEX),
    Player(KEY_PLAYER_H_INDEX),
}

fun SharedPreferences.getHIndex(type: HIndexType) = HIndex(this[type.key, 0] ?: 0, this[type.key + PlayStatPrefs.KEY_H_INDEX_N_SUFFIX, 0] ?: 0)

fun SharedPreferences.setHIndex(type: HIndexType, hIndex: HIndex) {
    this[type.key] = hIndex.h
    this[type.key + PlayStatPrefs.KEY_H_INDEX_N_SUFFIX] = hIndex.n
}

// endregion PLAY STATS

// region FORUMS

const val KEY_ADVANCED_DATES = "advancedForumDates"

// endregion FORUMS

//region LAST PLAY

const val KEY_LAST_PLAY_TIME = "last_play_time"
const val KEY_LAST_PLAY_DATE = "last_play_date"
const val KEY_LAST_PLAY_LOCATION = "last_play_location"
const val KEY_LAST_PLAY_PLAYERS = "last_play_players"

@Suppress("SpellCheckingInspection")
private const val SEPARATOR_RECORD = "OV=I=XrecordX=I=VO"

@Suppress("SpellCheckingInspection")
private const val SEPARATOR_FIELD = "OV=I=XfieldX=I=VO"

fun SharedPreferences.getLastPlayPlayers(): List<Player> {
    return this[KEY_LAST_PLAY_PLAYERS, ""]?.split(SEPARATOR_RECORD)?.filter { it.isNotBlank() }?.map {
        val x = it.split(SEPARATOR_FIELD)
        Player(x[0], x.getOrNull(1).orEmpty())
    }.orEmpty()
}

fun SharedPreferences.putLastPlayPlayers(players: List<PlayPlayer>?) {
    players?.let { list ->
        this[KEY_LAST_PLAY_PLAYERS] = list.joinToString(SEPARATOR_RECORD) {
            it.name + SEPARATOR_FIELD + it.username
        }
    }
}

//endregion LAST PLAY

@Suppress("SameParameterValue")
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

const val KEY_START_SCREEN = "advancedStartScreen"
const val START_SCREEN_LEGACY = "legacy"
const val START_SCREEN_SHELVES = "shelves"
