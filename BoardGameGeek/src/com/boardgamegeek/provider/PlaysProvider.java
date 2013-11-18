package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysProvider extends BasicProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		String fragment = uri.getFragment();
		if (BggContract.FRAGMENT_SIMPLE.equals(fragment)) {
			return buildSimpleSelection(uri);
		}
		return new SelectionBuilder().table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_ITEMS)
			.mapToTable(Plays._ID, getTable()).mapToTable(Plays.PLAY_ID, getTable())
			.mapToTable(PlayItems.NAME, Tables.PLAY_ITEMS).groupBy(Plays.PLAY_ID);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Plays.DEFAULT_SORT;
	}

	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Plays.PLAY_ID);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_PLAYS;
	}

	@Override
	protected String getTable() {
		return Tables.PLAYS;
	}

	@Override
	protected String getType(Uri uri) {
		return Plays.CONTENT_TYPE;
	}
}
