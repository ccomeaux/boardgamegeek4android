package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysIdPlayersProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long internalId = Plays.getInternalId(uri);
		return new SelectionBuilder()
			.table(Tables.PLAY_PLAYERS)
			.whereEquals(PlayPlayers._PLAY_ID, internalId);
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		long internalId = Plays.getInternalId(uri);
		return new SelectionBuilder()
			.table(Tables.PLAY_PLAYERS_JOIN_PLAYS)
			.whereEquals(PlayPlayers._PLAY_ID, internalId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return PlayPlayers.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "plays/#/players";
	}

	@Override
	protected String getType(Uri uri) {
		return PlayPlayers.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		long internalPlayId = Plays.getInternalId(uri);
		values.put(PlayPlayers._PLAY_ID, internalPlayId);
		long internalPlayerId = db.insertOrThrow(Tables.PLAY_PLAYERS, null, values);
		return Plays.buildPlayerUri(internalPlayId, internalPlayerId);
	}
}
