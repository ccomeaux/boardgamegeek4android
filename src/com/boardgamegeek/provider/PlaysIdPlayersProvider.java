package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysIdPlayersProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int playId = Plays.getPlayId(uri);
		return new SelectionBuilder().table(Tables.PLAY_PLAYERS).whereEquals(PlayPlayers.PLAY_ID, playId);
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
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		int playId = Plays.getPlayId(uri);
		values.put(PlayPlayers.PLAY_ID, playId);
		long rowId = db.insertOrThrow(Tables.PLAY_PLAYERS, null, values);
		return Plays.buildPlayerUri(playId, rowId);
	}
}
