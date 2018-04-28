package com.boardgamegeek.service

import android.content.Context
import android.content.SyncResult
import com.boardgamegeek.R
import com.boardgamegeek.clamp
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.queryCount
import com.boardgamegeek.queryStrings
import com.boardgamegeek.util.RemoteConfig

/**
 * Syncs a few buddies that haven't been updated in the longer while.
 */
class SyncBuddiesDetailOldest(context: Context, service: BggService, syncResult: SyncResult) : SyncBuddiesDetail(context, service, syncResult) {

    override val syncType = SyncService.FLAG_SYNC_BUDDIES

    override val notificationSummaryMessageId = R.string.sync_notification_buddies_oldest

    override val logMessage = "Syncing oldest buddies"

    private val days = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_DAYS)

    private val max = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_MAX)

    override fun fetchBuddyNames(): List<String> {
        val count = context.contentResolver.queryCount(Buddies.CONTENT_URI)
        if (count == 0) return emptyList()
        // attempt to sync all buddies every "days" days but no more than "max" at a time
        val limit = (count / days).clamp(1, max)
        return context.contentResolver.queryStrings(
                Buddies.CONTENT_URI,
                Buddies.BUDDY_NAME,
                sortOrder = "${Buddies.UPDATED} LIMIT $limit"
        )
    }
}
