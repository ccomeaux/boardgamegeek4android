@file:JvmName("SyncPrefUtils")

package com.boardgamegeek.pref

import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.remove
import com.boardgamegeek.extensions.set
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_BUDDIES
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_COMPLETE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_COLLECTION_PARTIAL
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_NEWEST_DATE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_OLDEST_DATE

class SyncPrefs {
    companion object {
        const val NAME = "com.boardgamegeek.sync"
        const val TIMESTAMP_COLLECTION_COMPLETE = "TIMESTAMP_COLLECTION_COMPLETE"
        const val TIMESTAMP_COLLECTION_PARTIAL = "TIMESTAMP_COLLECTION_PARTIAL"
        const val TIMESTAMP_BUDDIES = "TIMESTAMP_BUDDIES"
        const val TIMESTAMP_PLAYS_NEWEST_DATE = "TIMESTAMP_PLAYS_NEWEST_DATE"
        const val TIMESTAMP_PLAYS_OLDEST_DATE = "TIMESTAMP_PLAYS_OLDEST_DATE"

        @JvmStatic
        fun getPrefs(context: Context) = context.preferences(NAME)

        @JvmStatic
        fun migrate(context: Context) {
            val prefs = getPrefs(context)
            if (prefs.contains(TIMESTAMP_COLLECTION_COMPLETE)) return
            prefs.setLastCompleteCollectionTimestamp(getLong(context, "com.boardgamegeek.TIMESTAMP_COLLECTION_COMPLETE", 0L))
            prefs.setLastPartialCollectionTimestamp(getLong(context, "com.boardgamegeek.TIMESTAMP_COLLECTION_PARTIAL", 0L))
            prefs.setBuddiesTimestamp(getLong(context, "com.boardgamegeek.TIMESTAMP_BUDDIES", 0L))
            prefs.setPlaysNewestTimestamp(getLong(context, "com.boardgamegeek.TIMESTAMP_PLAYS_NEWEST_DATE", 0L))
            prefs.setPlaysOldestTimestamp(getLong(context, "com.boardgamegeek.TIMESTAMP_PLAYS_OLDEST_DATE", Long.MAX_VALUE))
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

fun SharedPreferences.getLastCompleteCollectionTimestamp() = this[TIMESTAMP_COLLECTION_COMPLETE, 0L]
        ?: 0L

fun SharedPreferences.setLastCompleteCollectionTimestamp(timestamp: Long = System.currentTimeMillis()) {
    this[TIMESTAMP_COLLECTION_COMPLETE] = timestamp
}

fun SharedPreferences.noPreviousCollectionSync(): Boolean {
    return getLastCompleteCollectionTimestamp() == 0L
}

fun SharedPreferences.getCurrentCollectionSyncTimestamp(): Long {
    return this["${TIMESTAMP_COLLECTION_COMPLETE}-CURRENT", 0L] ?: 0L
}

fun SharedPreferences.setCurrentCollectionSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
    this["${TIMESTAMP_COLLECTION_COMPLETE}-CURRENT"] = timestamp
}

fun SharedPreferences.getCompleteCollectionSyncTimestamp(subtype: String, status: String): Long {
    return this["${TIMESTAMP_COLLECTION_COMPLETE}.$subtype.$status", 0L] ?: 0L
}

fun SharedPreferences.setCompleteCollectionSyncTimestamp(subtype: String, status: String, timestamp: Long = System.currentTimeMillis()) {
    this["${TIMESTAMP_COLLECTION_COMPLETE}.$subtype.$status"] = timestamp
}

fun SharedPreferences.getLastPartialCollectionTimestamp() = this[TIMESTAMP_COLLECTION_PARTIAL, 0L]
        ?: 0L

fun SharedPreferences.setLastPartialCollectionTimestamp(timestamp: Long = System.currentTimeMillis()) {
    this[TIMESTAMP_COLLECTION_PARTIAL] = timestamp
}

fun SharedPreferences.getPartialCollectionSyncTimestamp(subtype: String): Long {
    val ts = this.getLastPartialCollectionTimestamp()
    return this["${TIMESTAMP_COLLECTION_PARTIAL}.$subtype", ts] ?: ts
}

fun SharedPreferences.setPartialCollectionSyncTimestamp(subtype: String, timestamp: Long = System.currentTimeMillis()) {
    this["${TIMESTAMP_COLLECTION_PARTIAL}.$subtype"] = timestamp
}

fun SharedPreferences.requestPartialSync() {
    setCurrentCollectionSyncTimestamp(getLastCompleteCollectionTimestamp())
}

fun SharedPreferences.clearCollection() {
    setLastCompleteCollectionTimestamp(0L)
    setCurrentCollectionSyncTimestamp(0L)
    val map = this.all
    map.keys
            .filter { it.startsWith("$TIMESTAMP_COLLECTION_COMPLETE.") }
            .forEach { this.remove(it) }
    setLastPartialCollectionTimestamp(0L)
    map.keys
            .filter { it.startsWith("$TIMESTAMP_COLLECTION_PARTIAL.") }
            .forEach { this.remove(it) }
}

// BUDDIES

fun SharedPreferences.clearBuddyListTimestamps() {
    setBuddiesTimestamp(0L)
}

fun SharedPreferences.getBuddiesTimestamp() = this[TIMESTAMP_BUDDIES, 0L]
        ?: 0L

fun SharedPreferences.setBuddiesTimestamp(timestamp: Long = System.currentTimeMillis()) {
    this[TIMESTAMP_BUDDIES] = timestamp
}

// PLAYS

fun SharedPreferences.getPlaysNewestTimestamp(): Long? {
    val l: Long? = this[TIMESTAMP_PLAYS_NEWEST_DATE]
    return when {
        l == null -> null
        l < 0L -> null
        else -> l
    }
}

fun SharedPreferences.setPlaysNewestTimestamp(timestamp: Long = System.currentTimeMillis()) {
    this[TIMESTAMP_PLAYS_NEWEST_DATE] = timestamp
}

fun SharedPreferences.getPlaysOldestTimestamp() = this[TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE]
        ?: Long.MAX_VALUE

fun SharedPreferences.setPlaysOldestTimestamp(timestamp: Long = System.currentTimeMillis()) {
    this[TIMESTAMP_PLAYS_OLDEST_DATE] = timestamp
}

fun SharedPreferences.isPlaysSyncUpToDate(): Boolean {
    return this.getPlaysOldestTimestamp() == 0L
}

fun SharedPreferences.clearPlaysTimestamps() {
    this.setPlaysNewestTimestamp(-1L)
    this.setPlaysOldestTimestamp(Long.MAX_VALUE)
}
