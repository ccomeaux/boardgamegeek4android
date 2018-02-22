package com.boardgamegeek.pref

import android.content.Context
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.service.SyncService

class SyncPrefUtils {
    companion object {
        @JvmStatic
        fun getLastCompleteCollectionTimestamp(context: Context) = Authenticator.getLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE)

        @JvmStatic
        fun setLastCompleteCollectionTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE, timestamp)
        }

        @JvmStatic
        fun getLastPartialCollectionTimestamp(context: Context) = Authenticator.getLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL)

        @JvmStatic
        fun setLastPartialCollectionTimestamp(context: Context, timestamp: Long = System.currentTimeMillis()) {
            Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, timestamp)
        }
    }
}