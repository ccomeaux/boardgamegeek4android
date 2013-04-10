package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdProvider extends BaseProvider {
	private static String GAME_POLLS_COUNT = "(SELECT COUNT(" + GamePolls.POLL_NAME + ") FROM " + Tables.GAME_POLLS
		+ " WHERE " + Tables.GAME_POLLS + "." + GamePolls.GAME_ID + "=" + Tables.GAMES + "." + Games.GAME_ID + ")";

	private final GamesProvider mProvider = new GamesProvider();

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAMES_JOIN_GAME_RANKS).mapToTable(Games._ID, Tables.GAMES)
			.mapToTable(Games.GAME_ID, Tables.GAMES).map(Games.POLLS_COUNT, GAME_POLLS_COUNT)
			.whereEquals(Tables.GAMES + "." + Games.GAME_ID, gameId).whereEqualsOrNull(GameRanks.GAME_RANK_ID, 1);
	}

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
