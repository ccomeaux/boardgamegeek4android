package com.boardgamegeek.provider;

import java.util.List;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPollsProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAME_POLLS).whereEquals(GamePolls.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return GamePolls.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/polls";
	}

	@Override
	protected String getType(Uri uri) {
		return GamePolls.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		int gameId = Games.getGameId(uri);
		values.put(GamePolls.GAME_ID, gameId);
		if (db.insertOrThrow(Tables.GAME_POLLS, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		String pollName = values.getAsString(GamePolls.POLL_NAME);
		return Games.buildPollsUri(gameId, pollName);
	}

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		List<String> pollIds = getList(db, builder, GamePolls._ID);
		for (String pollId : pollIds) {
			db.delete(Tables.GAME_POLL_RESULTS_RESULT,
					"pollresults_id IN (SELECT game_poll_results._id from game_poll_results WHERE poll_id=?)",
					new String[] { pollId });
			db.delete(Tables.GAME_POLL_RESULTS, GamePollResults.POLL_ID + "=?", new String[] { pollId });
		}
	}
}
