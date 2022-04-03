package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLORS
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.GameColors

class GamesColorsProviders : BaseProvider() {
    override fun getType(uri: Uri) = GameColors.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/$PATH_COLORS"

    override val defaultSortOrder = GameColors.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder().table(BggDatabase.Tables.GAME_COLORS)
    }
}
