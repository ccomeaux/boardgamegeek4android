package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollPollResults;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

import timber.log.Timber;

public class GamesIdSuggestedPlayerCountPollResultsProvider extends BaseProvider {
	private static final String TABLE = Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS;

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder()
			.table(TABLE)
			.whereEquals(GameSuggestedPlayerCountPollPollResults.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder()
			.table(Tables.POLLS_JOIN_GAMES)
			.whereEquals(Tables.GAMES + "." + GameSuggestedPlayerCountPollPollResults.GAME_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return GameSuggestedPlayerCountPollPollResults.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_GAMES + "/#/" + BggContract.PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS;
	}

	@Override
	protected String getType(Uri uri) {
		return GameSuggestedPlayerCountPollPollResults.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		int gameId = Games.getGameId(uri);
		values.put(GamePolls.GAME_ID, gameId);
		try {
			if (db.insertOrThrow(TABLE, null, values) != -1) {
				return Games.buildSuggestedPlayerCountPollResultsUri(gameId, values.getAsString(GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT));
			}
		} catch (SQLException e) {
			Timber.e(e, "Problem inserting poll result for game %s", gameId);
			notifyException(context, e);
		}
		return null;
	}
}
