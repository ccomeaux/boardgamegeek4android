package com.boardgamegeek.provider

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Games.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#"

    override fun query(
        db: SQLiteDatabase,
        uri: Uri, // content://com.boardgamegeek/games/13
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val gameId = Games.getGameId(uri)
        val qb = SQLiteQueryBuilder()
        qb.tables = Tables.GAMES
        qb.appendWhere("${Games.Columns.GAME_ID} = $gameId")
        return qb.query(db, projection, selection, selectionArgs, null, null, sortOrder)
    }
}
