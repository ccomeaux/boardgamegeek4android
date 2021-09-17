package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.util.SelectionBuilder

class GamesColorsProviders : BaseProvider() {
    override fun getType(uri: Uri) = BggContract.GameColors.CONTENT_ITEM_TYPE

    override val path = "${BggContract.PATH_GAMES}/${BggContract.PATH_COLORS}"

    override val defaultSortOrder = BggContract.GameColors.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder().table(BggDatabase.Tables.GAME_COLORS)
    }
}
