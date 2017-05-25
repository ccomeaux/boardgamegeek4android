package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdRankProvider extends BaseProvider {
	private static final String TABLE = Tables.GAME_RANKS;

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder()
			.table(TABLE)
			.whereEquals(Tables.GAME_RANKS + "." + GameRanks.GAME_ID, Games.getGameId(uri));
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return new SelectionBuilder()
			.table(Tables.GAMES_RANKS_JOIN_GAMES)
			.whereEquals(Tables.GAME_RANKS + "." + GameRanks.GAME_ID, Games.getGameId(uri));
	}

	@Override
	protected String getDefaultSortOrder() {
		return GameRanks.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/ranks";
	}

	@Override
	protected String getType(Uri uri) {
		return GameRanks.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		values.put(GameRanks.GAME_ID, Games.getGameId(uri));
		long rowId = db.insertOrThrow(TABLE, null, values);
		if (rowId != -1) {
			return GameRanks.buildGameRankUri((int) rowId);
		}
		return null;
	}
}
