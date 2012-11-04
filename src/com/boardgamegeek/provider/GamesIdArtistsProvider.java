package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdArtistsProvider extends BaseProvider {
	private static final String TABLE = Tables.GAMES_ARTISTS;

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAMES_ARTISTS_JOIN_ARTISTS).mapToTable(Artists._ID, Tables.ARTISTS)
			.mapToTable(Artists.ARTIST_ID, Tables.ARTISTS).mapToTable(SyncColumns.UPDATED, Tables.ARTISTS)
			.whereEquals(Tables.GAMES_ARTISTS + "." + GamesArtists.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GamesArtists.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Artists.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/artists";
	}

	@Override
	protected String getType(Uri uri) {
		return Artists.CONTENT_ITEM_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		values.put(GamesArtists.GAME_ID, Games.getGameId(uri));
		long rowId = db.insertOrThrow(TABLE, null, values);
		return Games.buildArtistUri(rowId);
	}
}
