package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class GamesProvider extends BasicProvider {

	@Override
	protected String getDefaultSortOrder() {
		return Games.DEFAULT_SORT;
	}

	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Games.GAME_ID);
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
}
