package com.boardgamegeek.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int playId = Plays.getPlayId(uri);
		return new SelectionBuilder().table(Tables.PLAY_ITEMS_JOIN_PLAYS).mapToTable(BaseColumns._ID, Tables.PLAYS)
			.mapToTable(Plays.PLAY_ID, Tables.PLAYS).whereEquals(Tables.PLAYS + "." + Plays.PLAY_ID, playId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int playId = Plays.getPlayId(uri);
		return new SelectionBuilder().table(Tables.PLAYS).whereEquals(Plays.PLAY_ID, String.valueOf(playId));
	}

	@Override
	protected String getPath() {
		return "plays/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Plays.CONTENT_ITEM_TYPE;
	}
}
