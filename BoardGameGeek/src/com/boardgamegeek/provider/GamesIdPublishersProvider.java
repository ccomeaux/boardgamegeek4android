package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggDatabase.GamesPublishers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPublishersProvider extends BaseProvider {
	private static final String TABLE = Tables.GAMES_PUBLISHERS;

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		final int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAMES_PUBLISHERS_JOIN_PUBLISHERS)
				.mapToTable(Publishers._ID, Tables.PUBLISHERS).mapToTable(Publishers.PUBLISHER_ID, Tables.PUBLISHERS)
				.mapToTable(SyncColumns.UPDATED, Tables.PUBLISHERS)
				.whereEquals(Tables.GAMES_PUBLISHERS + "." + GamesPublishers.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GamesPublishers.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Publishers.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/publishers";
	}

	@Override
	protected String getType(Uri uri) {
		return Publishers.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		long rowId = db.insertOrThrow(TABLE, null, values);
		return Games.buildPublisherUri(rowId);
	}
}
