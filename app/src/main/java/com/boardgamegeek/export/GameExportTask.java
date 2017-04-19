package com.boardgamegeek.export;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.export.model.Game;
import com.boardgamegeek.provider.BggContract.Games;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

public class GameExportTask extends JsonExportTask<Game> {
	public GameExportTask(Context context, Uri uri) {
		super(context, Constants.TYPE_GAMES, uri);
	}

	@Override
	protected Cursor getCursor(Context context) {
		return context.getContentResolver().query(
			Games.CONTENT_URI,
			Game.PROJECTION,
			null, null, null);
	}

	@Override
	protected void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer) {
		Game game = Game.fromCursor(cursor);
		game.addColors(context);
		if (game.hasColors()) {
			gson.toJson(game, Game.class, writer);
		}
	}
}
