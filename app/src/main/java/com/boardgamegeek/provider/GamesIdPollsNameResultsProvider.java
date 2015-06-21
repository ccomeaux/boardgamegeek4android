package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

import timber.log.Timber;

public class GamesIdPollsNameResultsProvider extends BaseProvider {
	private static final String DEFAULT_KEY = "X";

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		return new SelectionBuilder().table(Tables.POLLS_JOIN_POLL_RESULTS)
			.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS).whereEquals(GamePolls.GAME_ID, gameId)
			.whereEquals(GamePolls.POLL_NAME, pollName);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);
		return new SelectionBuilder()
			.table(Tables.GAME_POLL_RESULTS)
			.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
			.where("poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)",
				String.valueOf(gameId), pollName);
	}

	@Override
	protected String getDefaultSortOrder() {
		return GamePollResults.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/polls/*/results";
	}

	@Override
	protected String getType(Uri uri) {
		return GamePollResults.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		int gameId = Games.getGameId(uri);
		String pollName = Games.getPollName(uri);

		SelectionBuilder builder = new GamesIdPollsNameProvider().buildSimpleSelection(Games.buildPollsUri(gameId,
			pollName));
		int pollId = queryInt(db, builder, GamePolls._ID);
		values.put(GamePollResults.POLL_ID, pollId);

		String key = values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS);
		if (TextUtils.isEmpty(key)) {
			key = DEFAULT_KEY;
		}
		values.put(GamePollResults.POLL_RESULTS_KEY, key);

		try {
			if (db.insertOrThrow(Tables.GAME_POLL_RESULTS, null, values) != -1) {
				return Games.buildPollResultsUri(gameId, pollName,
					values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS));
			}
		} catch (SQLException e) {
			Timber.e(e, "Problem inserting poll %2$s for game %1$s", gameId, pollName);
			notifyException(context, e);
		}
		return null;
	}
}
