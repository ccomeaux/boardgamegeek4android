package com.boardgamegeek.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPollsNameResultsResultProvider extends BaseProvider {
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		return new SelectionBuilder().table(Tables.POLLS_RESULTS_RESULT_JOIN_POLLS_RESULTS_JOIN_POLLS)
			.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS_RESULT).whereEquals(GamePolls.GAME_ID, gameId)
			.whereEquals(GamePollResults.POLL_NAME, pollName);
	}

	@Override
	protected String getPath() {
		return "games/#/polls/*/results/result";
	}

	@Override
	protected String getType(Uri uri) {
		return GamePollResultsResult.CONTENT_TYPE;
	}
}
