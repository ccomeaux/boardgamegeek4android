package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.queryStrings
import com.boardgamegeek.util.SelectionBuilder

/**
 * Syncs all buddies that haven't been updated completely.
 */
class SyncBuddiesDetailUnupdated(application: BggApplication, service: BggService, syncResult: SyncResult) : SyncBuddiesDetail(application, service, syncResult) {

    override val syncType = SyncService.FLAG_SYNC_BUDDIES

    override val notificationSummaryMessageId = R.string.sync_notification_buddies_unupdated

    override val logMessage = "Syncing unupdated buddies..."

    override fun fetchBuddyNames(): List<String> {
        return context.contentResolver.queryStrings(
                Buddies.CONTENT_URI,
                Buddies.BUDDY_NAME,
                SelectionBuilder.whereZeroOrNull(Buddies.UPDATED))
    }
}
