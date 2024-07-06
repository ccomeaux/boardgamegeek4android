package com.boardgamegeek.pref

import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.remove
import com.boardgamegeek.extensions.set
import com.boardgamegeek.io.BggService
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_BUDDIES
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_COMPLETE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_COMPLETE_CURRENT
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_PARTIAL
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_NEWEST_DATE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_OLDEST_DATE

class SyncPrefs {
    companion object {
        const val NAME = "com.boardgamegeek.sync"
        const val TIMESTAMP_COLLECTION_COMPLETE = "TIMESTAMP_COLLECTION_COMPLETE"
        const val TIMESTAMP_COLLECTION_COMPLETE_CURRENT = "${TIMESTAMP_COLLECTION_COMPLETE}-CURRENT"
        const val TIMESTAMP_COLLECTION_PARTIAL = "TIMESTAMP_COLLECTION_PARTIAL"
        const val TIMESTAMP_BUDDIES = "TIMESTAMP_BUDDIES"
        const val TIMESTAMP_PLAYS_NEWEST_DATE = "TIMESTAMP_PLAYS_NEWEST_DATE"
        const val TIMESTAMP_PLAYS_OLDEST_DATE = "TIMESTAMP_PLAYS_OLDEST_DATE"

        fun getPrefs(context: Context) = context.preferences(NAME)

        fun migrate(context: Context) {
            val prefs = getPrefs(context)
            if (prefs.contains(TIMESTAMP_COLLECTION_COMPLETE)) return
            prefs[TIMESTAMP_COLLECTION_COMPLETE] = getLong(context, "com.boardgamegeek.TIMESTAMP_COLLECTION_COMPLETE", 0L)
            prefs[TIMESTAMP_COLLECTION_PARTIAL] = (getLong(context, "com.boardgamegeek.TIMESTAMP_COLLECTION_PARTIAL", 0L))
            prefs.setBuddiesTimestamp(getLong(context, "com.boardgamegeek.TIMESTAMP_BUDDIES", 0L))
            prefs[TIMESTAMP_PLAYS_NEWEST_DATE] = getLong(context, "com.boardgamegeek.TIMESTAMP_PLAYS_NEWEST_DATE", 0L)
            prefs[TIMESTAMP_PLAYS_OLDEST_DATE] = getLong(context, "com.boardgamegeek.TIMESTAMP_PLAYS_OLDEST_DATE", Long.MAX_VALUE)
        }

        private fun getLong(context: Context, key: String, defaultValue: Long): Long {
            AccountManager.get(context).apply {
                val account = Authenticator.getAccount(this)
                return if (account == null)
                    defaultValue
                else
                    getUserData(account, key)?.toLongOrNull() ?: defaultValue
            }
        }
    }
}

// COLLECTION

fun getCompleteCollectionTimestampKey(subtype: BggService.ThingSubtype?, status: String): String {
    return "$TIMESTAMP_COLLECTION_COMPLETE.${subtype?.code.orEmpty()}.$status"
}

fun getPartialCollectionTimestampKey(subtype: BggService.ThingSubtype?) = "${TIMESTAMP_COLLECTION_PARTIAL}.${subtype?.code.orEmpty()}"

fun SharedPreferences.clearCollection() {
    this[TIMESTAMP_COLLECTION_COMPLETE] = 0L
    this[TIMESTAMP_COLLECTION_COMPLETE_CURRENT] = 0L
    val map = this.all
    map.keys
        .filter { it.startsWith("$TIMESTAMP_COLLECTION_COMPLETE.") }
        .forEach { this.remove(it) }
    this[TIMESTAMP_COLLECTION_PARTIAL] = 0L
    map.keys
        .filter { it.startsWith("$TIMESTAMP_COLLECTION_PARTIAL.") }
        .forEach { this.remove(it) }
}

// BUDDIES

fun SharedPreferences.clearBuddyListTimestamps() {
    setBuddiesTimestamp(0L)
}

fun SharedPreferences.getBuddiesTimestamp() = this[TIMESTAMP_BUDDIES, 0L] ?: 0L

fun SharedPreferences.setBuddiesTimestamp(timestamp: Long = System.currentTimeMillis()) {
    this[TIMESTAMP_BUDDIES] = timestamp
}

// PLAYS

fun SharedPreferences.clearPlaysTimestamps() {
    this[TIMESTAMP_PLAYS_NEWEST_DATE] = 0L
    this[TIMESTAMP_PLAYS_OLDEST_DATE] = Long.MAX_VALUE
}
