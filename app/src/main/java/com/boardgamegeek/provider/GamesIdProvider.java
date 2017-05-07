package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdProvider extends BaseProvider {
	private final GamesProvider provider = new GamesProvider();

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String GAME_POLLS_COUNT = "(SELECT COUNT(" + GamePolls.POLL_NAME + ") FROM " + Tables.GAME_POLLS
			+ " WHERE " + Tables.GAME_POLLS + "." + GamePolls.GAME_ID + "=" + Tables.GAMES + "." + Games.GAME_ID + ")";
		return new SelectionBuilder()
			.table(Tables.GAMES)
			.map(Games.POLLS_COUNT, GAME_POLLS_COUNT)
			.whereEquals(Tables.GAMES + "." + Games.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return provider.buildSimpleSelection(uri).whereEquals(Games.GAME_ID, gameId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(provider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Games.CONTENT_ITEM_TYPE;
	}
}
