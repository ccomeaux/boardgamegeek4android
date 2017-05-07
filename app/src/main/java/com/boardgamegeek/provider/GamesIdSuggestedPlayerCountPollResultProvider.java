package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollPollResults;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdSuggestedPlayerCountPollResultProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String playerCount = uri.getLastPathSegment();
		return new SelectionBuilder().table(Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS)
			.whereEquals(GameSuggestedPlayerCountPollPollResults.GAME_ID, gameId)
			.whereEquals(GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT, playerCount);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_GAMES + "/#/" + BggContract.PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS + "/*";
	}

	@Override
	protected String getType(Uri uri) {
		return GameSuggestedPlayerCountPollPollResults.CONTENT_ITEM_TYPE;
	}
}
