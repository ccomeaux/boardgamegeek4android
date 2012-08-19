package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdProvider extends BaseProvider {
	final GamesProvider mProvider = new GamesProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return mProvider.buildSimpleSelection(uri).whereEquals(Games.GAME_ID, gameId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(mProvider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Games.CONTENT_ITEM_TYPE;
	}
}
