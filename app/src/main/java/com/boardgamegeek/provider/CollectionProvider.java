package com.boardgamegeek.provider;

import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionProvider extends BasicProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		SelectionBuilder builder = new SelectionBuilder()
			.table(Tables.COLLECTION_JOIN_GAMES_JOIN_PLAYS)
			.mapToTable(Collection._ID, Tables.COLLECTION)
			.mapToTable(Collection.GAME_ID, Tables.COLLECTION)
			.mapToTable(Collection.UPDATED, Tables.COLLECTION)
			.mapToTable(Collection.UPDATED_LIST, Tables.COLLECTION)
			.mapIfNull(Games.GAME_RANK, String.valueOf(Integer.MAX_VALUE))
			.mapAsMax(Plays.MAX_DATE, Plays.DATE);

		String groupBy = uri.getQueryParameter(BggContract.QUERY_KEY_GROUP_BY);
		if (!TextUtils.isEmpty(groupBy)) {
			builder.groupBy(groupBy);
		} else {
			builder.groupBy(Collection.COLLECTION_ID);
		}
		return builder;
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
