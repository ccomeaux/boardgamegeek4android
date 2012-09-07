package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggDatabase.GamesMechanics;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdMechanicsProvider extends BaseProvider {
	private static final String TABLE = Tables.GAMES_MECHANICS;

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		final int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAMES_MECHANICS_JOIN_MECHANICS)
			.mapToTable(Mechanics._ID, Tables.MECHANICS).mapToTable(Mechanics.MECHANIC_ID, Tables.MECHANICS)
			.whereEquals(Tables.GAMES_MECHANICS + "." + GamesMechanics.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		final int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GamesMechanics.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Mechanics.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/mechanics";
	}

	@Override
	protected String getType(Uri uri) {
		return Mechanics.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		values.put(GamesMechanics.GAME_ID, Games.getGameId(uri));
		long rowId = db.insertOrThrow(TABLE, null, values);
		return Games.buildMechanicUri(rowId);
	}
}
