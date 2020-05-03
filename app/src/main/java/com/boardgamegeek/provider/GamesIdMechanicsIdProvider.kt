package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.GamesMechanics
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdMechanicsIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Mechanics.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_MECHANICS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val mechanicId = ContentUris.parseId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_MECHANICS)
                .whereEquals(GamesMechanics.GAME_ID, gameId)
                .whereEquals(GamesMechanics.MECHANIC_ID, mechanicId)
    }
}
