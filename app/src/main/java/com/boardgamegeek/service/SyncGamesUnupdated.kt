package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.whereZeroOrNull
import com.boardgamegeek.io.BggService
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.util.RemoteConfig
import com.boardgamegeek.util.SelectionBuilder

/**
 * Syncs all games in the collection that have not been updated completely.
 */
class SyncGamesUnupdated(application: BggApplication, service: BggService, syncResult: SyncResult) : SyncGames(application, service, syncResult) {

    override val syncType = SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD

    override val exitLogMessage = "...no more unupdated games"

    override val selection: String? = "games.${Games.UPDATED}".whereZeroOrNull()

    override val maxFetchCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_FETCH_MAX_UNUPDATED)

    override val notificationSummaryMessageId = R.string.sync_notification_games_unupdated

    override fun getIntroLogMessage(gamesPerFetch: Int) = "Syncing $gamesPerFetch unupdated games in the collection..."
}
