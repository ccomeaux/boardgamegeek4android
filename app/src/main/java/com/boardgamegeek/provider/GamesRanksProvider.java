package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesRanksProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.GAME_RANKS);
	}

	@Override
	protected String getDefaultSortOrder() {
		return GameRanks.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/ranks";
	}

	@Override
	protected String getType(Uri uri) {
		return GameRanks.CONTENT_TYPE;
	}
}
