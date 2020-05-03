package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.GamesDesigners.DESIGNER_ID
import com.boardgamegeek.provider.BggDatabase.GamesDesigners.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdDesignersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Designers.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_DESIGNERS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val designerId = ContentUris.parseId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_DESIGNERS)
                .whereEquals(GAME_ID, gameId)
                .whereEquals(DESIGNER_ID, designerId)
    }
}
