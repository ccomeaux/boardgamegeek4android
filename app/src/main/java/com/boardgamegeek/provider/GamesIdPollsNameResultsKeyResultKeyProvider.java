package com.boardgamegeek.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPollsNameResultsKeyResultKeyProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		String key = Games.getPollResultsKey(uri);
		String key2 = Games.getPollResultsResultKey(uri);
		return new SelectionBuilder()
				.table(Tables.GAME_POLL_RESULTS_RESULT)
				.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
				.where("pollresults_id = (SELECT game_poll_results._id FROM game_poll_results WHERE game_poll_results.pollresults_key=? AND game_poll_results.poll_id =(SELECT game_poll_results._id FROM game_poll_results WHERE game_poll_results.poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)))",
						key, String.valueOf(gameId), pollName)
				.whereEquals(GamePollResultsResult.POLL_RESULTS_RESULT_KEY, key2);
	}

	@Override
	protected String getPath() {
		return "games/#/polls/*/results/*/result/*";
	}

	@Override
	protected String getType(Uri uri) {
		return GamePollResultsResult.CONTENT_ITEM_TYPE;
	}
}
