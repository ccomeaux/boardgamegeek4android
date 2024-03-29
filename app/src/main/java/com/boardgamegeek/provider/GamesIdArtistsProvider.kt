package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.PATH_ARTISTS
import com.boardgamegeek.provider.BggContract.Companion.PATH_GAMES
import com.boardgamegeek.provider.BggDatabase.GamesArtists.GAME_ID
import com.boardgamegeek.provider.BggDatabase.Tables

class GamesIdArtistsProvider : BaseProvider() {
    override fun getType(uri: Uri) = Artists.CONTENT_ITEM_TYPE

    override val path = "$PATH_GAMES/#/$PATH_ARTISTS"

    override val defaultSortOrder = Artists.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder().table(Tables.GAMES_ARTISTS).whereEquals(GAME_ID, gameId)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        val gameId = Games.getGameId(uri)
        return SelectionBuilder()
            .table(Tables.GAMES_ARTISTS_JOIN_ARTISTS)
            .mapToTable(BaseColumns._ID, Tables.ARTISTS)
            .mapToTable(Artists.Columns.ARTIST_ID, Tables.ARTISTS)
            .mapToTable(Artists.Columns.UPDATED, Tables.ARTISTS)
            .whereEquals("${Tables.GAMES_ARTISTS}.$GAME_ID", gameId)
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        values.put(GAME_ID, Games.getGameId(uri))
        val rowId = db.insertOrThrow(Tables.GAMES_ARTISTS, null, values)
        return Games.buildArtistUri(rowId)
    }
}
