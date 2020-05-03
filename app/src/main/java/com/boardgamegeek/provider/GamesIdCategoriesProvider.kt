package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.GamesCategories.CATEGORY_ID
import com.boardgamegeek.provider.BggDatabase.GamesCategories.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class GamesIdCategoriesProvider : BaseProvider() {
    override fun getType(uri: Uri) = Categories.CONTENT_TYPE

    override val path = "$PATH_GAMES/#/$PATH_CATEGORIES"

    override val defaultSortOrder = Categories.DEFAULT_SORT

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder().table(Tables.GAMES_CATEGORIES).whereEquals(GAME_ID, gameId)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
                .table(Tables.GAMES_CATEGORIES_JOIN_CATEGORIES)
                .mapToTable(Categories._ID, Tables.CATEGORIES)
                .mapToTable(CATEGORY_ID, Tables.CATEGORIES)
                .mapToTable(SyncColumns.UPDATED, Tables.CATEGORIES)
                .whereEquals("${Tables.GAMES_CATEGORIES}.$GAME_ID", gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAMES_CATEGORIES, null, values)
        return Games.buildCategoryUri(rowId)
    }
}
