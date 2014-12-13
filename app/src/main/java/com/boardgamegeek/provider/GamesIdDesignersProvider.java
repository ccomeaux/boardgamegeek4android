package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdDesignersProvider extends BaseProvider {
	private static final String TABLE = Tables.GAMES_DESIGNERS;

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAMES_DESIGNERS_JOIN_DESIGNERS)
			.mapToTable(Designers._ID, Tables.DESIGNERS).mapToTable(Designers.DESIGNER_ID, Tables.DESIGNERS)
			.mapToTable(SyncColumns.UPDATED, Tables.DESIGNERS)
			.whereEquals(Tables.GAMES_DESIGNERS + "." + GamesDesigners.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GamesDesigners.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Designers.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/designers";
	}

	@Override
	protected String getType(Uri uri) {
		return Designers.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		values.put(GamesDesigners.GAME_ID, Games.getGameId(uri));
		long rowId = db.insertOrThrow(TABLE, null, values);
		return Games.buildDesignersUri(rowId);
	}
}
