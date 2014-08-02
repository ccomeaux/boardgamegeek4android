package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionProvider extends BasicProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return new SelectionBuilder().table(getExpandedTable()).mapToTable(Collection._ID, Tables.COLLECTION)
			.mapToTable(Collection.GAME_ID, Tables.COLLECTION).mapToTable(Collection.UPDATED, Tables.COLLECTION)
			.mapToTable(Collection.UPDATED_LIST, Tables.COLLECTION)
			.whereEqualsOrNull(GameRanks.GAME_RANK_TYPE, "subtype").groupBy(Tables.COLLECTION + "." + Collection._ID);
	}

	protected String getExpandedTable() {
		return Tables.COLLECTION_JOIN_GAMES_JOIN_GAME_RANKS;
	}

	@Override
	protected String getDefaultSortOrder() {
		return Collection.DEFAULT_SORT;
	}

	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Collection.COLLECTION_ID);
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
