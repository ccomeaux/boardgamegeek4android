package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdRankIdProvider extends BaseProvider {
	GamesIdRankProvider mProvider = new GamesIdRankProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int rankId = GameRanks.getRankId(uri);
		return mProvider.buildSimpleSelection(uri).whereEquals(GameRanks.GAME_RANK_ID, rankId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(mProvider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return GameRanks.CONTENT_ITEM_TYPE;
	}
}
