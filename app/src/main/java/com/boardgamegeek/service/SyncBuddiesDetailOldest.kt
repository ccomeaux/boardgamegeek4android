package com.boardgamegeek.service

import android.content.Context
import android.content.SyncResult
import com.boardgamegeek.R
import com.boardgamegeek.constrain
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.queryCount
import com.boardgamegeek.queryStrings

/**
 * Syncs roughly 7% of buddies that haven't been updated in the longer while.
 */
class SyncBuddiesDetailOldest(context: Context, service: BggService, syncResult: SyncResult) : SyncBuddiesDetail(context, service, syncResult) {

    override val syncType: Int
        get() = SyncService.FLAG_SYNC_BUDDIES

    override val notificationSummaryMessageId: Int
        get() = R.string.sync_notification_buddies_oldest

    override val logMessage: String
        get() = "Syncing oldest buddies"

    override fun fetchBuddyNames(): List<String> {
        val count = context.contentResolver.queryCount(Buddies.CONTENT_URI)
        if (count == 0) return emptyList()
        // attempt to sync all buddies every 2 weeks but no more than 16
        return context.contentResolver.queryStrings(
                Buddies.CONTENT_URI, Buddies.BUDDY_NAME, sortOrder = "${Buddies.UPDATED} LIMIT ${(count / 14).constrain(1, 16)}"
        )
    }
}
