package com.boardgamegeek.pref

import android.content.Context
import com.boardgamegeek.PreferenceHelper
import com.boardgamegeek.PreferenceHelper.get
import com.boardgamegeek.PreferenceHelper.set
import com.boardgamegeek.auth.Authenticator

class SyncPrefs {
    companion object {
        private const val TIMESTAMP_COLLECTION_COMPLETE = "TIMESTAMP_COLLECTION_COMPLETE"
        private const val AUTH_TIMESTAMP_COLLECTION_COMPLETE = "com.boardgamegeek.TIMESTAMP_COLLECTION_COMPLETE"
        private const val AUTH_TIMESTAMP_COLLECTION_PARTIAL = "com.boardgamegeek.TIMESTAMP_COLLECTION_PARTIAL"
        private const val AUTH_TIMESTAMP_BUDDIES = "com.boardgamegeek.TIMESTAMP_BUDDIES"
        private const val AUTH_TIMESTAMP_PLAYS_NEWEST_DATE = "com.boardgamegeek.TIMESTAMP_PLAYS_NEWEST_DATE"
        private const val AUTH_TIMESTAMP_PLAYS_OLDEST_DATE = "com.boardgamegeek.TIMESTAMP_PLAYS_OLDEST_DATE"

        private fun getPrefs(context: Context) = PreferenceHelper.customPrefs(context, "com.boardgamegeek.sync")

        private fun collectionStatusKey(subtype: String, status: String) = "$TIMESTAMP_COLLECTION_COMPLETE.$status.$subtype"

        @JvmStatic
        fun getLastCompleteCollectionTimestamp(context: Context) = Authenticator.getLong(context, AUTH_TIMESTAMP_COLLECTION_COMPLETE)

        @JvmStatic
        fun setLastCompleteCollectionTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            Authenticator.putLong(context, AUTH_TIMESTAMP_COLLECTION_COMPLETE, timestamp)
        }

        @JvmStatic
        fun getLastPartialCollectionTimestamp(context: Context) = Authenticator.getLong(context, AUTH_TIMESTAMP_COLLECTION_PARTIAL)

        @JvmStatic
        fun setLastPartialCollectionTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            Authenticator.putLong(context, AUTH_TIMESTAMP_COLLECTION_PARTIAL, timestamp)
        }

        fun getCurrentCollectionSyncTimestamp(context: Context): Long {
            return getPrefs(context)["$TIMESTAMP_COLLECTION_COMPLETE-CURRENT", 0L] ?: 0L
        }

        fun setCurrentCollectionSyncTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)["$TIMESTAMP_COLLECTION_COMPLETE-CURRENT"] = timestamp
        }

        fun getCollectionSyncTimestamp(context: Context, status: String, subtype: String): Long {
            return getPrefs(context)[collectionStatusKey(subtype, status), 0L] ?: 0L
        }

        fun setCollectionSyncTimestamp(context: Context, status: String, subtype: String, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)[collectionStatusKey(subtype, status)] = timestamp
        }

        @JvmStatic
        fun clearCollection(context: Context) {
            setLastCompleteCollectionTimestamp(context, 0L)
            setLastPartialCollectionTimestamp(context, 0L)
            setCurrentCollectionSyncTimestamp(context, 0L)
            val prefs = getPrefs(context)
            val map = prefs.all
            map.keys
                    .filter { it.startsWith("$TIMESTAMP_COLLECTION_COMPLETE.") }
                    .forEach { prefs.edit().remove(it).apply() }
        }

        @JvmStatic
        fun getBuddiesTimestamp(context: Context) = Authenticator.getLong(context, AUTH_TIMESTAMP_BUDDIES)

        @JvmStatic
        fun setBuddiesTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            Authenticator.putLong(context, AUTH_TIMESTAMP_BUDDIES, timestamp)
        }

        @JvmStatic
        fun clearBuddyListTimestamps(context: Context) {
            setBuddiesTimestamp(context, 0L)
        }

        @JvmStatic
        fun getPlaysNewestTimestamp(context: Context) = Authenticator.getLong(context, AUTH_TIMESTAMP_PLAYS_NEWEST_DATE)

        @JvmStatic
        fun setPlaysNewestTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            Authenticator.putLong(context, AUTH_TIMESTAMP_PLAYS_NEWEST_DATE, timestamp)
        }

        @JvmStatic
        fun getPlaysOldestTimestamp(context: Context) = Authenticator.getLong(context, AUTH_TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE)

        @JvmStatic
        fun setPlaysOldestTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            Authenticator.putLong(context, AUTH_TIMESTAMP_PLAYS_OLDEST_DATE, timestamp)
        }

        @JvmStatic
        fun clearPlaysTimestamps(context: Context) {
            setPlaysNewestTimestamp(context, 0L)
            setPlaysOldestTimestamp(context, Long.MAX_VALUE)
        }

        @JvmStatic
        fun isPlaysSyncUpToDate(context: Context): Boolean {
            return getPlaysOldestTimestamp(context) == 0L
        }
    }
}