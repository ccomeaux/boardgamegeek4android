package com.boardgamegeek.export

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.export.model.Color
import com.boardgamegeek.export.model.Game
import com.boardgamegeek.extensions.load
import com.boardgamegeek.provider.BggContract.GameColors
import com.boardgamegeek.provider.BggContract.Games
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter

class GameExportTask(context: Context, uri: Uri) : JsonExportTask<Game>(context, Constants.TYPE_GAMES, Constants.TYPE_GAMES_DESCRIPTION, uri) {
    override val version: Int
        get() = 1

    override fun getCursor(context: Context): Cursor? {
        return context.contentResolver.query(
                Games.CONTENT_URI,
                arrayOf(Games.GAME_ID),
                null, null, null)
    }

    override fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter) {
        val gameId = cursor.getInt(0)

        val colors = mutableListOf<Color>()
        context.contentResolver.load(
                Games.buildColorsUri(gameId),
                arrayOf(GameColors.COLOR)
        )?.use {
            while (it.moveToNext()) {
                it.getString(0).orEmpty().also { color ->
                    if (color.isNotBlank()) colors.add(Color(color))
                }
            }
        }

        if (colors.isNotEmpty()) {
            gson.toJson(Game(gameId, colors), Game::class.java, writer)
        }
    }
}