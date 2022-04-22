package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.get
import com.boardgamegeek.io.BggService
import com.boardgamegeek.repository.UserRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber

abstract class SyncBuddiesDetail(application: BggApplication, service: BggService, syncResult: SyncResult) :
    SyncTask(application, service, syncResult) {
    private val repository = UserRepository(application)

    /**
     * Returns a log message to use for debugging purposes.
     */
    protected abstract val logMessage: String

    /**
     * Get a list of usernames to sync.
     */
    protected abstract fun fetchBuddyNames(): List<String>

    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_PAUSE_MILLIS)

    override fun execute() {
        Timber.i(logMessage)
        try {
            if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] != true) {
                Timber.i("...buddies not set to sync")
                return
            }

            var count = 0
            val names = fetchBuddyNames()
            Timber.i("...found %,d buddies to update", names.size)
            if (names.isNotEmpty()) {
                for (name in names) {
                    if (isCancelled) {
                        Timber.i("...canceled while syncing buddies")
                        break
                    }

                    val notificationMessage = context.getString(R.string.sync_notification_buddy, name)
                    updateProgressNotification(notificationMessage)

                    runBlocking {
                        try {
                            repository.refresh(name)
                            syncResult.stats.numUpdates++
                            count++
                        } catch (e: Exception) {
                            if (e is HttpException) {
                                showError(notificationMessage, e.code())
                            } else {
                                showError(notificationMessage, e)
                            }
                            syncResult.stats.numIoExceptions++
                            cancel()
                        }
                    }

                    if (wasSleepInterrupted(fetchPauseMillis, showNotification = false)) break
                }
            } else {
                Timber.i("...no buddies to update")
            }
            Timber.i("...saved %,d records", count)
        } finally {
            Timber.i("...complete!")
        }
    }
}
