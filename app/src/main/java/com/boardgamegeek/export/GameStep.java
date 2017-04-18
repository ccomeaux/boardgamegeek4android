package com.boardgamegeek.export;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.export.model.Color;
import com.boardgamegeek.export.model.Game;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ResolverUtils;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.util.ArrayList;
import java.util.List;

public class GameStep implements Step {
	@NonNull
	@Override
	public String getName() {
		return "bgg4a-games";
	}

	@Override
	public int getVersion() {
		return 1;
	}

	@NonNull
	@Override
	public String getDescription(@NonNull Context context) {
		return context.getString(R.string.backup_type_game);
	}

	@Nullable
	@Override
	public Cursor getCursor(@NonNull Context context) {
		return context.getContentResolver().query(
			Games.CONTENT_URI,
			Game.PROJECTION,
			null, null, null);
	}

	@Override
	public void writeJsonRecord(@NonNull Context context, @NonNull Cursor cursor, @NonNull Gson gson, @NonNull JsonWriter writer) {
		Game game = Game.fromCursor(cursor);
		game.addColors(context);
		if (game.hasColors()) {
			gson.toJson(game, Game.class, writer);
		}
	}

	@Override
	public void initializeImport(Context context) {
	}

	@Override
	public void importRecord(@NonNull Context context, @NonNull Gson gson, @NonNull JsonReader reader) {
		Game game = gson.fromJson(reader, Game.class);

		ContentResolver resolver = context.getContentResolver();

		int gameId = game.getGameId();
		if (ResolverUtils.rowExists(resolver, Games.buildGameUri(gameId))) {
			final Uri gameColorsUri = Games.buildColorsUri(gameId);

			resolver.delete(gameColorsUri, null, null);

			List<ContentValues> values = new ArrayList<>();
			for (Color color : game.getColors()) {
				if (!TextUtils.isEmpty(color.getColor())) {
					ContentValues cv = new ContentValues();
					cv.put(GameColors.COLOR, color.getColor());
					values.add(cv);
				}
			}

			if (values.size() > 0) {
				ContentValues[] array = {};
				resolver.bulkInsert(gameColorsUri, values.toArray(array));
			}
		}
	}
}
