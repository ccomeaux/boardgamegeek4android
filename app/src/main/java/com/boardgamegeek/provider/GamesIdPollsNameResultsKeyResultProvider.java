package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.DataUtils;
import com.boardgamegeek.util.SelectionBuilder;

import timber.log.Timber;

public class GamesIdPollsNameResultsKeyResultProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		String players = Games.getPollResultsKey(uri);
		return new SelectionBuilder()
			.table(Tables.POLL_RESULTS_JOIN_POLL_RESULTS_RESULT)
			.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS_RESULT)
			.whereEquals(GamePollResults.POLL_RESULTS_PLAYERS, players)
			.where("poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)",
				String.valueOf(gameId), pollName);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		String key = Games.getPollResultsKey(uri);
		return new SelectionBuilder()
			.table(Tables.GAME_POLL_RESULTS_RESULT)
			.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
			.whereEquals(GamePollResults.POLL_RESULTS_KEY, key)
			.where(
				"game_poll_results._id FROM game_poll_results WHERE game_poll_results.poll_id =(SELECT game_poll_results._id FROM game_poll_results WHERE game_poll_results.poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)",
				String.valueOf(gameId), pollName);
	}

	@Override
	protected String getDefaultSortOrder() {
		return GamePollResultsResult.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/polls/*/results/*/result";
	}

	@Override
	protected String getType(Uri uri) {
		return GamePollResultsResult.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		String players = Games.getPollResultsKey(uri);

		SelectionBuilder builder = new GamesIdPollsNameResultsKeyProvider().buildSimpleSelection(Games
			.buildPollResultsUri(gameId, pollName, players));
		int id = queryInt(db, builder, GamePollResultsResult._ID);
		values.put(GamePollResultsResult.POLL_RESULTS_ID, id);

		String key = DataUtils.generatePollResultsKey(
			values.getAsString(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL),
			values.getAsString(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE));
		values.put(GamePollResultsResult.POLL_RESULTS_RESULT_KEY, key);

		try {
			if (db.insertOrThrow(Tables.GAME_POLL_RESULTS_RESULT, null, values) != -1) {
				return Games.buildPollResultsResultUri(gameId, pollName, players,
					values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS));
			}
		} catch (SQLException e) {
			Timber.e(e, "Problem inserting poll %2$s %3$s for game %1$s", gameId, pollName, players);
			notifyException(context, e);
		}
		return null;
	}
}
