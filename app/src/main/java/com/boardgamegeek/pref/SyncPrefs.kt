package com.boardgamegeek.pref

import android.accounts.AccountManager
import android.content.Context
import android.text.TextUtils
import com.boardgamegeek.PreferenceHelper
import com.boardgamegeek.PreferenceHelper.get
import com.boardgamegeek.PreferenceHelper.set
import com.boardgamegeek.auth.Authenticator

class SyncPrefs {
    companion object {
        private const val TIMESTAMP_COLLECTION_COMPLETE = "TIMESTAMP_COLLECTION_COMPLETE"
        private const val TIMESTAMP_COLLECTION_PARTIAL = "TIMESTAMP_COLLECTION_PARTIAL"
        private const val TIMESTAMP_BUDDIES = "TIMESTAMP_BUDDIES"
        private const val TIMESTAMP_PLAYS_NEWEST_DATE = "TIMESTAMP_PLAYS_NEWEST_DATE"
        private const val TIMESTAMP_PLAYS_OLDEST_DATE = "TIMESTAMP_PLAYS_OLDEST_DATE"

        private fun getPrefs(context: Context) = PreferenceHelper.customPrefs(context, "com.boardgamegeek.sync")

        @JvmStatic
        fun migrate(context: Context) {
            if (getPrefs(context).contains(TIMESTAMP_COLLECTION_COMPLETE)) return
            setLastCompleteCollectionTimestamp(context, getLong(context, "com.boardgamegeek.TIMESTAMP_COLLECTION_COMPLETE", 0L))
            setLastPartialCollectionTimestamp(context, getLong(context, "com.boardgamegeek.TIMESTAMP_COLLECTION_PARTIAL", 0L))
            setBuddiesTimestamp(context, getLong(context, "com.boardgamegeek.TIMESTAMP_BUDDIES", 0L))
            setPlaysNewestTimestamp(context, getLong(context, "com.boardgamegeek.TIMESTAMP_PLAYS_NEWEST_DATE", 0L))
            setPlaysOldestTimestamp(context, getLong(context, "com.boardgamegeek.TIMESTAMP_PLAYS_OLDEST_DATE", Long.MAX_VALUE))
        }

        @JvmStatic
        fun getLastCompleteCollectionTimestamp(context: Context) = getPrefs(context)[TIMESTAMP_COLLECTION_COMPLETE, 0L]
                ?: 0L

        fun setLastCompleteCollectionTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)[TIMESTAMP_COLLECTION_COMPLETE] = timestamp
        }

        fun getCurrentCollectionSyncTimestamp(context: Context): Long {
            return getPrefs(context)["$TIMESTAMP_COLLECTION_COMPLETE-CURRENT", 0L] ?: 0L
        }

        fun setCurrentCollectionSyncTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)["$TIMESTAMP_COLLECTION_COMPLETE-CURRENT"] = timestamp
        }

        fun getCompleteCollectionSyncTimestamp(context: Context, status: String, subtype: String): Long {
            return getPrefs(context)["$TIMESTAMP_COLLECTION_COMPLETE.$status.$subtype", 0L] ?: 0L
        }

        fun setCompleteCollectionSyncTimestamp(context: Context, status: String, subtype: String, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)["$TIMESTAMP_COLLECTION_COMPLETE.$status.$subtype"] = timestamp
        }

        @JvmStatic
        fun getLastPartialCollectionTimestamp(context: Context) = getPrefs(context)[TIMESTAMP_COLLECTION_PARTIAL, 0L]
                ?: 0L

        fun setLastPartialCollectionTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)[TIMESTAMP_COLLECTION_PARTIAL] = timestamp
        }

        fun getPartialCollectionSyncTimestamp(context: Context, subtype: String): Long {
            val ts = getLastPartialCollectionTimestamp(context)
            return getPrefs(context)["$TIMESTAMP_COLLECTION_PARTIAL.$subtype", ts] ?: ts
        }

        fun setPartialCollectionSyncTimestamp(context: Context, subtype: String, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)["$TIMESTAMP_COLLECTION_PARTIAL.$subtype"] = timestamp
        }

        @JvmStatic
        fun clearCollection(context: Context) {
            setLastCompleteCollectionTimestamp(context, 0L)
            setCurrentCollectionSyncTimestamp(context, 0L)
            val prefs = getPrefs(context)
            val map = prefs.all
            map.keys
                    .filter { it.startsWith("$TIMESTAMP_COLLECTION_COMPLETE.") }
                    .forEach { prefs.edit().remove(it).apply() }
            setLastPartialCollectionTimestamp(context, 0L)
            map.keys
                    .filter { it.startsWith("$TIMESTAMP_COLLECTION_PARTIAL.") }
                    .forEach { prefs.edit().remove(it).apply() }
        }

        @JvmStatic
        fun getBuddiesTimestamp(context: Context) = getPrefs(context)[TIMESTAMP_BUDDIES, 0L] ?: 0L

        fun setBuddiesTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)[TIMESTAMP_BUDDIES] = timestamp
        }

        @JvmStatic
        fun clearBuddyListTimestamps(context: Context) {
            setBuddiesTimestamp(context, 0L)
        }

        @JvmStatic
        fun getPlaysNewestTimestamp(context: Context) = getPrefs(context)[TIMESTAMP_PLAYS_NEWEST_DATE, 0L] ?: 0L

        fun setPlaysNewestTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)[TIMESTAMP_PLAYS_NEWEST_DATE] = timestamp
        }

        @JvmStatic
        fun getPlaysOldestTimestamp(context: Context) = getPrefs(context)[TIMESTAMP_PLAYS_OLDEST_DATE, 0L] ?: 0L

        fun setPlaysOldestTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            getPrefs(context)[TIMESTAMP_PLAYS_OLDEST_DATE] = timestamp
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

        private fun getLong(context: Context, key: String, defaultValue: Long): Long {
            val accountManager = AccountManager.get(context)
            val account = Authenticator.getAccount(accountManager) ?: return defaultValue
            val s = accountManager.getUserData(account, key)
            return if (TextUtils.isEmpty(s)) defaultValue else java.lang.Long.parseLong(s)
        }
    }
}