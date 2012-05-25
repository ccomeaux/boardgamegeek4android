package com.boardgamegeek.provider;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPollsNameResultsKeyProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		String players = Games.getPollResultsKey(uri);
		return new SelectionBuilder().table(Tables.POLLS_JOIN_POLL_RESULTS)
				.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS).whereEquals(GamePolls.GAME_ID, gameId)
				.whereEquals(GamePolls.POLL_NAME, pollName).whereEquals(GamePollResults.POLL_RESULTS_PLAYERS, players);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		String key = Games.getPollResultsKey(uri);
		return new SelectionBuilder()
				.table(Tables.GAME_POLL_RESULTS)
				.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
				.where("poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)",
						String.valueOf(gameId), pollName).whereEquals(GamePollResults.POLL_RESULTS_PLAYERS, key);
	}

	@Override
	protected String getPath() {
		return "games/#/polls/*/results/*";
	}

	@Override
	protected String getType(Uri uri) {
		return GamePollResults.CONTENT_ITEM_TYPE;
	}

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		new GamesIdPollsNameResultsProvider().deleteChildren(db, builder);
	}
}
