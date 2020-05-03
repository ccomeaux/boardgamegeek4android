package com.boardgamegeek.provider

import android.content.ContentUris
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.GamesCategories.CATEGORY_ID
import com.boardgamegeek.provider.BggDatabase.GamesCategories.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdCategoriesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_CATEGORIES/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        val categoryId = ContentUris.parseId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_CATEGORIES)
                .whereEquals(GAME_ID, gameId)
                .whereEquals(CATEGORY_ID, categoryId)
    }
}
