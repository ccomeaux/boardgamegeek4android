package com.boardgamegeek.provider;

import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesProvider extends BasicProvider {

	@Override
	protected String getDefaultSortOrder() {
		return Games.DEFAULT_SORT;
	}

	@Override
	protected String getInsertedIdColumn() {
		return Games.GAME_ID;
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_GAMES;
	}

	@Override
	protected String getTable() {
		return Tables.GAMES;
	}

	@Override
	protected String getType(Uri uri) {
		return Games.CONTENT_TYPE;
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		SelectionBuilder builder = new SelectionBuilder();
		if (BggContract.FRAGMENT_PLAYS.equals(uri.getFragment())) {
			builder
				.table(Tables.GAMES_JOIN_PLAYS)
				.mapIfNull(Games.GAME_RANK, String.valueOf(Integer.MAX_VALUE))
				.mapAsSum(Plays.SUM_QUANTITY, Plays.QUANTITY, Tables.PLAYS)
				.mapAsMax(Plays.MAX_DATE, Plays.DATE);
		} else {
			builder
				.table(Tables.GAMES_JOIN_COLLECTION)
				.mapToTable(Games.GAME_ID, Tables.GAMES);
		}
		builder.mapToTable(Games.UPDATED, Tables.GAMES);


		String groupBy = uri.getQueryParameter(BggContract.QUERY_KEY_GROUP_BY);
		if (!TextUtils.isEmpty(groupBy)) {
			builder.groupBy(groupBy);
		} else {
			builder.groupBy(Games.GAME_ID);
		}
		return builder;
	}
}
