package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesRanksIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int rankId = GameRanks.getRankId(uri);
		return new SelectionBuilder().table(Tables.GAME_RANKS).whereEquals(GameRanks.GAME_RANK_ID, rankId);
	}

	@Override
	protected String getPath() {
		return "games/ranks/#";
	}

	@Override
	protected String getType(Uri uri) {
		return GameRanks.CONTENT_ITEM_TYPE;
	}
}
