package com.boardgamegeek.export;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.export.model.Game;
import com.boardgamegeek.provider.BggContract;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

public class GameExporter extends Exporter {
	@Override
	public String getFileName() {
		return "games.json";
	}

	@Override
	public Cursor getCursor(Context context) {
		return context.getContentResolver().query(
			BggContract.Games.CONTENT_URI,
			Game.PROJECTION,
			null, null, null);
	}

	@Override
	public void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer) {
		Game game = Game.fromCursor(cursor);
		game.addColors(context);
		if (game.hasColors()) {
			gson.toJson(game, Game.class, writer);
		}
	}
}
