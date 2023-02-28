package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.queryStrings
import com.boardgamegeek.extensions.whereZeroOrNull
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.repository.UserRepository

/**
 * Syncs all buddies that haven't been updated completely.
 */
class SyncBuddiesDetailUnupdated(application: BggApplication, syncResult: SyncResult, repository: UserRepository) :
    SyncBuddiesDetail(application, syncResult, repository) {

    override val syncType = SyncService.FLAG_SYNC_BUDDIES

    override val notificationSummaryMessageId = R.string.sync_notification_buddies_unupdated

    override val logMessage = "Syncing unupdated buddies..."

    override fun fetchBuddyNames(): List<String> {
        return context.contentResolver.queryStrings(
            Buddies.CONTENT_URI,
            Buddies.Columns.BUDDY_NAME,
            Buddies.Columns.UPDATED.whereZeroOrNull()
        )
    }
}
