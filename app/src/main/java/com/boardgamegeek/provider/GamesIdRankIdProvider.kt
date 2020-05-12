package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdRankIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameRanks.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_RANKS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val rankId = GameRanks.getRankId(uri)
        return SelectionBuilder()
                .table(Tables.GAME_RANKS)
                .whereEquals("${Tables.GAME_RANKS}.$GAME_ID", Games.getGameId(uri))
                .whereEquals(GameRanks.GAME_RANK_ID, rankId)
    }
}
