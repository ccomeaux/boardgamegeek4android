package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.GamesCategories;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdColorsProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAME_COLORS).whereEquals(GamesCategories.GAME_ID, gameId);
	}

	@Override
	protected String getPath() {
		return "games/#/colors";
	}

	@Override
	protected String getType(Uri uri) {
		return GameColors.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		int gameId = Games.getGameId(uri);
		values.put(GameColors.GAME_ID, gameId);
		if (db.insertOrThrow(Tables.GAME_COLORS, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Games.buildColorsUri(gameId, values.getAsString(GameColors.COLOR));
	}
}
