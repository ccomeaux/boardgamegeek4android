package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_RANKS
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesRanksIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameRanks.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/$PATH_RANKS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val rankId = GameRanks.getRankId(uri)
        return SelectionBuilder()
            .table(Tables.GAME_RANKS)
            .whereEquals(GameRanks.Columns.GAME_RANK_ID, rankId)
    }
}
