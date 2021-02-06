package com.boardgamegeek.service

import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.os.bundleOf
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.provider.BggContract

class SyncService : Service() {
    override fun onCreate() {
        synchronized(SYNC_ADAPTER_LOCK) {
            if (syncAdapter == null) {
                syncAdapter = SyncAdapter((application as BggApplication))
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return syncAdapter?.syncAdapterBinder
    }

    companion object {
        const val EXTRA_SYNC_TYPE = "com.boardgamegeek.SYNC_TYPE"
        const val ACTION_CANCEL_SYNC = "com.boardgamegeek.ACTION_SYNC_CANCEL"

        const val FLAG_SYNC_NONE = 0
        const val FLAG_SYNC_COLLECTION_DOWNLOAD = 1
        const val FLAG_SYNC_COLLECTION_UPLOAD = 1 shl 1
        const val FLAG_SYNC_BUDDIES = 1 shl 2
        const val FLAG_SYNC_PLAYS_DOWNLOAD = 1 shl 3
        const val FLAG_SYNC_PLAYS_UPLOAD = 1 shl 4
        const val FLAG_SYNC_GAMES = 1 shl 5
        const val FLAG_SYNC_COLLECTION = FLAG_SYNC_COLLECTION_DOWNLOAD or FLAG_SYNC_COLLECTION_UPLOAD or FLAG_SYNC_GAMES
        const val FLAG_SYNC_PLAYS = FLAG_SYNC_PLAYS_DOWNLOAD or FLAG_SYNC_PLAYS_UPLOAD
        const val FLAG_SYNC_ALL = FLAG_SYNC_COLLECTION or FLAG_SYNC_BUDDIES or FLAG_SYNC_PLAYS

        private val SYNC_ADAPTER_LOCK = Any()
        private var syncAdapter: SyncAdapter? = null

        @JvmStatic
        fun sync(context: Context?, syncType: Int) {
            Authenticator.getAccount(context)?.let { account ->
                val extras = bundleOf(
                        ContentResolver.SYNC_EXTRAS_MANUAL to true,
                        EXTRA_SYNC_TYPE to syncType
                )
                ContentResolver.requestSync(account, BggContract.CONTENT_AUTHORITY, extras)
            }
        }
    }
}