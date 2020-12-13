package com.boardgamegeek.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.core.content.contentValuesOf
import com.boardgamegeek.export.model.Game
import com.boardgamegeek.extensions.rowExists
import com.boardgamegeek.provider.BggContract.GameColors
import com.boardgamegeek.provider.BggContract.Games
import com.google.gson.Gson
import com.google.gson.stream.JsonReader

class GameImportTask(context: Context, uri: Uri) : JsonImportTask<Game>(context, Constants.TYPE_GAMES, Constants.TYPE_GAMES_DESCRIPTION, uri) {
    override fun parseItem(gson: Gson, reader: JsonReader): Game {
        return gson.fromJson(reader, Game::class.java)
    }

    override fun importRecord(item: Game, version: Int) {
        if (context.contentResolver.rowExists(Games.buildGameUri(item.id))) {
            val gameColorsUri = Games.buildColorsUri(item.id)
            context.contentResolver.delete(gameColorsUri, null, null)
            val values = mutableListOf<ContentValues>()
            item.colors.filter { it.color.isNotBlank() }.forEach { color ->
                values.add(contentValuesOf(GameColors.COLOR to color.color))
            }
            if (values.isNotEmpty()) {
                context.contentResolver.bulkInsert(gameColorsUri, values.toTypedArray())
            }
        }
    }
}