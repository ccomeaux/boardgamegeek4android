package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.queryCount
import com.boardgamegeek.extensions.queryStrings
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.util.RemoteConfig

/**
 * Syncs a few buddies that haven't been updated in the longer while.
 */
class SyncBuddiesDetailOldest(application: BggApplication, service: BggService, syncResult: SyncResult) : SyncBuddiesDetail(application, service, syncResult) {

    override val syncType = SyncService.FLAG_SYNC_BUDDIES

    override val notificationSummaryMessageId = R.string.sync_notification_buddies_oldest

    override val logMessage = "Syncing oldest buddies"

    private val days = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_DAYS)

    private val max = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_BUDDIES_MAX)

    override fun fetchBuddyNames(): List<String> {
        val count = context.contentResolver.queryCount(Buddies.CONTENT_URI)
        if (count == 0) return emptyList()
        // attempt to sync all buddies every "days" days but no more than "max" at a time
        val limit = (count / days).coerceIn(1, max)
        return context.contentResolver.queryStrings(
                Buddies.CONTENT_URI,
                Buddies.Columns.BUDDY_NAME,
                sortOrder = "${Buddies.Columns.UPDATED} LIMIT $limit"
        )
    }
}
