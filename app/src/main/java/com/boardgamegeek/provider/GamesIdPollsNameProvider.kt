package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdPollsNameProvider : BaseProvider() {
    override fun getType(uri: Uri) = GamePolls.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_POLLS/*"

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val pollName = Games.getPollName(uri)
        return SelectionBuilder()
                .table(Tables.GAME_POLLS)
                .whereEquals(GamePolls.GAME_ID, gameId)
                .whereEquals(GamePolls.POLL_NAME, pollName)
    }
}
