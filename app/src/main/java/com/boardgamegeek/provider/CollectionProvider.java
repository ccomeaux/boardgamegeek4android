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
			.table(Tables.COLLECTION_JOIN_GAMES)
			.mapToTable(Collection._ID, Tables.COLLECTION)
			.mapToTable(Collection.GAME_ID, Tables.COLLECTION)
			.mapToTable(Collection.UPDATED, Tables.COLLECTION)
			.mapToTable(Collection.UPDATED_LIST, Tables.COLLECTION)
			.mapToTable(Collection.PRIVATE_INFO_QUANTITY, Tables.COLLECTION)
			.mapIfNull(Games.GAME_RANK, String.valueOf(Integer.MAX_VALUE))
			.map(Plays.MAX_DATE, String.format("(SELECT MAX(%s) FROM %s WHERE %s.%s=%s.%s)", Plays.DATE, Tables.PLAYS, Tables.PLAYS, Plays.OBJECT_ID, Tables.GAMES, Games.GAME_ID));

		String groupBy = uri.getQueryParameter(BggContract.QUERY_KEY_GROUP_BY);
		if (!TextUtils.isEmpty(groupBy)) {
			builder.groupBy(groupBy);
		} else {
			builder.groupBy(Collection.GAME_ID);
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
