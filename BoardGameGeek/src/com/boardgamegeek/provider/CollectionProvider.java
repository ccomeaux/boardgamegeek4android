package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionProvider extends BasicProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return new SelectionBuilder().table(getExpandedTable()).mapToTable(Collection._ID, Tables.COLLECTION)
			.mapToTable(Collection.GAME_ID, Tables.COLLECTION).mapToTable(Collection.UPDATED, Tables.COLLECTION)
			.mapToTable(Collection.UPDATED_LIST, Tables.COLLECTION);
	}

	protected String getExpandedTable() {
		return Tables.COLLECTION_JOIN_GAMES;
	}

	@Override
	protected String getDefaultSortOrder() {
		return Collection.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_COLLECTION;
	}

	@Override
	protected String getTable() {
		return Tables.COLLECTION;
	}

	@Override
	protected String getType(Uri uri) {
		return Collection.CONTENT_TYPE;
	}
}
