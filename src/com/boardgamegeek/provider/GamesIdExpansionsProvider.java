package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdExpansionsProvider extends BaseProvider {

	private static final String TABLE = Tables.GAMES_EXPANSIONS;

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAMES_EXPANSIONS_JOIN_EXPANSIONS)
			.mapToTable(GamesExpansions._ID, Tables.GAMES_EXPANSIONS)
			.mapToTable(GamesExpansions.GAME_ID, Tables.GAMES_EXPANSIONS)
			.whereEquals(Tables.GAMES_EXPANSIONS + "." + Games.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GamesExpansions.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return GamesExpansions.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/expansions";
	}

	@Override
	protected String getType(Uri uri) {
		return GamesExpansions.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		values.put(GamesExpansions.GAME_ID, Games.getGameId(uri));
		long rowId = db.insertOrThrow(TABLE, null, values);
		return Games.buildExpansionUri(rowId);
	}
}
