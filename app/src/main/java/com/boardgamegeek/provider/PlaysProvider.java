package com.boardgamegeek.provider;

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
			return new SelectionBuilder().table(Tables.PLAY_ITEMS_JOIN_PLAYS).mapToTable(Plays._ID, getTable())
				.mapToTable(Plays.PLAY_ID, getTable());
		} else if (BggContract.FRAGMENT_SUM.equals(fragment)) {
			return new SelectionBuilder().table(Tables.PLAY_ITEMS_JOIN_PLAYS).groupBy(PlayItems.OBJECT_ID)
				.mapToTable(Plays._ID, getTable()).mapToTable(Plays.PLAY_ID, getTable());
		}
		return new SelectionBuilder().table(Tables.PLAY_ITEMS_JOIN_PLAYS_JOIN_GAMES).mapToTable(Plays._ID, getTable())
			.mapToTable(Plays.PLAY_ID, getTable()).mapToTable(Plays.UPDATED, Tables.PLAYS)
			.mapToTable(Plays.UPDATED_LIST, Tables.PLAYS);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Plays.DEFAULT_SORT;
	}

	@Override
	protected String getInsertedIdColumn() {
		return Plays.PLAY_ID;
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
