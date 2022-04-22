package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Companion.PATH_POLLS
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdPollsNameProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamePolls.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_POLLS/*"

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        return SelectionBuilder()
            .table(Tables.GAME_POLLS)
            .whereEquals(GamePolls.Columns.GAME_ID, gameId)
            .whereEquals(GamePolls.Columns.POLL_NAME, pollName)
    }
}
