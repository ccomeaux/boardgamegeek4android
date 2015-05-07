package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

import timber.log.Timber;

public class GamesIdPollsProvider extends BaseProvider {
	private static final String TABLE = Tables.GAME_POLLS;

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GamePolls.GAME_ID, gameId);
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
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		int gameId = Games.getGameId(uri);
		values.put(GamePolls.GAME_ID, gameId);
		try {
			if (db.insertOrThrow(TABLE, null, values) != -1) {
				return Games.buildPollsUri(gameId, values.getAsString(GamePolls.POLL_NAME));
			}
		} catch (SQLException e) {
			Timber.e(e, "Problem inserting poll for game %1$s", gameId);
			notifyException(context, e);
		}
		return null;
	}
}
