package com.boardgamegeek.service

import android.accounts.Account
import android.content.SyncResult
import androidx.annotation.StringRes
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.UserDao
import com.boardgamegeek.extensions.PREFERENCES_KEY_SYNC_BUDDIES
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.User
import com.boardgamegeek.pref.getBuddiesTimestamp
import com.boardgamegeek.pref.setBuddiesTimestamp
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.util.RemoteConfig
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Syncs the list of buddies. Only runs every few days.
 */
class SyncBuddiesList(application: BggApplication, service: BggService, syncResult: SyncResult, private val account: Account) : SyncTask(application, service, syncResult) {
    @StringRes
    private var currentDetailResId: Int = 0
    private val userDao = UserDao(this.application)
    private var updateTimestamp = System.currentTimeMillis()

    override val syncType = SyncService.FLAG_SYNC_BUDDIES

    override val notificationSummaryMessageId = R.string.sync_notification_buddies_list

    private val fetchIntervalInDays = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_FETCH_INTERVAL_DAYS)

    override fun execute() {
        Timber.i("Syncing list of buddies...")
        try {
            if (prefs[PREFERENCES_KEY_SYNC_BUDDIES, false] != true) {
                Timber.i("...buddies not set to sync")
                return
            }

            val lastCompleteSync = syncPrefs.getBuddiesTimestamp()
            if (lastCompleteSync >= 0 && !lastCompleteSync.isOlderThan(fetchIntervalInDays, TimeUnit.DAYS)) {
                Timber.i("...skipping; we synced already within the last $fetchIntervalInDays days")
                return
            }

            updateTimestamp = System.currentTimeMillis()

            updateNotification(R.string.sync_notification_buddies_list_downloading)
            val user = requestUser() ?: return

            updateNotification(R.string.sync_notification_buddies_list_storing)
            persistUser(user)

            updateNotification(R.string.sync_notification_buddies_list_pruning)
            pruneOldBuddies()

            syncPrefs.setBuddiesTimestamp(updateTimestamp)
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun updateNotification(@StringRes detailResId: Int) {
        currentDetailResId = detailResId
        updateProgressNotification(context.getString(detailResId))
    }

    private fun requestUser(): User? {
        var user: User? = null
        val call = service.user(account.name, 1, 1)
        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                showError(context.getString(currentDetailResId), response.code())
                syncResult.stats.numIoExceptions++
                cancel()
            }
            user = response.body()
        } catch (e: IOException) {
            showError(context.getString(currentDetailResId), e)
            syncResult.stats.numIoExceptions++
            cancel()
        }

        return user
    }

    private fun persistUser(user: User) {
        var count = userDao.saveUser(user.id, user.name, false)

        (user.buddies?.buddies ?: emptyList()).forEach { buddy ->
            count += userDao.saveUser(buddy.id.toIntOrNull() ?: BggContract.INVALID_ID, buddy.name)
        }

        syncResult.stats.numEntries += count.toLong()
        Timber.i("Synced %,d buddies", count)
    }

    private fun pruneOldBuddies() {
        val count = userDao.deleteUsersAsOf(updateTimestamp)
        syncResult.stats.numDeletes += count.toLong()
        Timber.i("Pruned %,d users who are no longer buddies", count)
    }
}
