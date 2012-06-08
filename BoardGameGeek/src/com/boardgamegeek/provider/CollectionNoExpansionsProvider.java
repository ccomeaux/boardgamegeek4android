package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionNoExpansionsProvider extends CollectionProvider {
	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return super.buildExpandedSelection(uri).having("SUM(" + GamesExpansions.INBOUND + ")=0 OR " + GamesExpansions.INBOUND + " IS NULL");
	}

	@Override
	protected String getJoinTable() {
		return super.getJoinTable() + createJoin(Tables.GAMES_EXPANSIONS, GamesExpansions.GAME_ID);
	}
	@Override
	protected String getPath() {
		return super.getPath() + "/" + BggContract.PATH_NOEXPANSIONS;
	}
}
