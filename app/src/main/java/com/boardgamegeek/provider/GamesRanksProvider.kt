package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesRanksProvider : BaseProvider() {
    override fun getType(uri: Uri) = GameRanks.CONTENT_TYPE

    override fun getPath() = "$PATH_GAMES/$PATH_RANKS"

    override fun getDefaultSortOrder() = GameRanks.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder().table(Tables.GAME_RANKS)
    }
}
