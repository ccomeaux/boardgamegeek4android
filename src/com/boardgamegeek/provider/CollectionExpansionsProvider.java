package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionExpansionsProvider extends CollectionProvider {
	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return super.buildExpandedSelection(uri).having("SUM(" + GamesExpansions.INBOUND + ")>0");
	}

	@Override
	protected String getExpandedTable() {
		return Tables.COLLECTION_JOIN_GAMES_JOIN_GAME_RANKS_JOIN_EXPANSIONS;
	}

	@Override
	protected String getPath() {
		return super.getPath() + "/" + BggContract.PATH_EXPANSIONS;
	}
}
