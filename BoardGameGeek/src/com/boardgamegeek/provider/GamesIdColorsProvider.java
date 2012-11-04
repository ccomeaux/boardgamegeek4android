package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdColorsProvider extends BaseProvider {
	private static final String TABLE = Tables.GAME_COLORS;

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GameColors.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return GameColors.DEFAULT_SORT;
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
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		int gameId = Games.getGameId(uri);
		values.put(GameColors.GAME_ID, gameId);
		if (db.insertOrThrow(TABLE, null, values) != -1) {
			return Games.buildColorsUri(gameId, values.getAsString(GameColors.COLOR));
		}
		return null;
	}
}
