package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionProvider extends BasicProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return new SelectionBuilder().table(getJoinTable()).mapToTable(Collection._ID, Tables.COLLECTION)
				.mapToTable(Collection.GAME_ID, Tables.COLLECTION).whereEquals(GameRanks.GAME_RANK_ID, 1)
				.groupBy(Collection.COLLECTION_ID);
	}

	protected String getJoinTable() {
		return Tables.COLLECTION + createJoin(Tables.GAMES, Games.GAME_ID)
				+ createJoin(Tables.GAME_RANKS, GameRanks.GAME_ID);
	}

	protected static String createJoin(String table2, String column) {
		return " LEFT OUTER JOIN " + table2 + " ON " + Tables.COLLECTION + "." + column + "=" + table2 + "." + column;
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
