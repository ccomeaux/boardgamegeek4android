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
import com.boardgamegeek.repository.*
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SyncService : Service() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var collectionItemRepository: CollectionItemRepository
    @Inject lateinit var gameRepository: GameRepository
    @Inject lateinit var gameCollectionRepository: GameCollectionRepository
    @Inject @Named("withAuth") lateinit var httpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        synchronized(SYNC_ADAPTER_LOCK) {
            if (syncAdapter == null) {
                syncAdapter = SyncAdapter(
                    (application as BggApplication),
                    authRepository,
                    collectionItemRepository,
                    gameRepository,
                    gameCollectionRepository,
                    httpClient,
                )
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
        const val FLAG_SYNC_GAMES = 1 shl 5
        const val FLAG_SYNC_COLLECTION = FLAG_SYNC_COLLECTION_DOWNLOAD or FLAG_SYNC_COLLECTION_UPLOAD or FLAG_SYNC_GAMES
        const val FLAG_SYNC_ALL = FLAG_SYNC_COLLECTION

        private val SYNC_ADAPTER_LOCK = Any()
        private var syncAdapter: SyncAdapter? = null

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
