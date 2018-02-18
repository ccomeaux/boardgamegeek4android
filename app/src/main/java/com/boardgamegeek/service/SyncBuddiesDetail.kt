package com.boardgamegeek.service

import android.content.Context
import android.content.SyncResult
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.User
import com.boardgamegeek.model.persister.BuddyPersister
import com.boardgamegeek.util.PreferencesUtils
import timber.log.Timber
import java.io.IOException

abstract class SyncBuddiesDetail(context: Context, service: BggService, syncResult: SyncResult) : SyncTask(context, service, syncResult) {
    private var notificationMessage: String? = null

    /**
     * Returns a log message to use for debugging purposes.
     */
    protected abstract val logMessage: String

    /**
     * Get a list of usernames to sync.
     */
    protected abstract fun fetchBuddyNames(): List<String>

    override fun execute() {
        Timber.i(logMessage)
        try {
            if (!PreferencesUtils.getSyncBuddies(context)) {
                Timber.i("...buddies not set to sync")
                return
            }

            val persister = BuddyPersister(context)
            var count = 0
            val names = fetchBuddyNames()
            Timber.i("...found %,d buddies to update", names.size)
            if (names.isNotEmpty()) {
                for (name in names) {
                    if (isCancelled) {
                        Timber.i("...canceled while syncing buddies")
                        break
                    }

                    notificationMessage = context.getString(R.string.sync_notification_buddy, name)
                    updateProgressNotification(notificationMessage)

                    val user = requestUser(name) ?: break

                    persister.saveUser(user)
                    syncResult.stats.numUpdates++
                    count++

                    if (wasSleepInterrupted(SLEEP_MILLIS)) break
                }
            } else {
                Timber.i("...no buddies to update")
            }
            Timber.i("...saved %,d records", count)
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun requestUser(name: String): User? {
        var user: User? = null
        try {
            val call = service.user(name)
            val response = call.execute()
            if (!response.isSuccessful) {
                showError(notificationMessage!!, response.code())
                syncResult.stats.numIoExceptions++
                cancel()
            }
            user = response.body()
        } catch (e: IOException) {
            showError(notificationMessage!!, e)
            syncResult.stats.numIoExceptions++
            cancel()
        }

        return user
    }

    companion object {
        private const val SLEEP_MILLIS = 2000L
    }
}
