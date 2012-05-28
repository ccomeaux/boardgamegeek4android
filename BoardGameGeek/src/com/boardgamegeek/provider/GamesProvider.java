package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.GAMES);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Games.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games";
	}

	@Override
	protected String getType(Uri uri) {
		return Games.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		if (db.insertOrThrow(Tables.GAMES, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Games.buildGameUri(values.getAsInteger(Games.GAME_ID));
	}

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		// TODO after upgrading to API 8, use cascading deletes (http://stackoverflow.com/questions/2545558)
		Cursor cursor = builder.query(db, new String[] { Games.GAME_ID }, null);
		try {
			while (cursor.moveToNext()) {
				int gameId = cursor.getInt(0);
				String[] gameArg = new String[] { String.valueOf(gameId) };
				db.delete(Tables.GAME_RANKS, GameRanks.GAME_ID + "=?", gameArg);
				db.delete(Tables.COLLECTION, Collection.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_DESIGNERS, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_ARTISTS, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_PUBLISHERS, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_MECHANICS, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_CATEGORIES, Games.GAME_ID + "=?", gameArg);
				db.delete(Tables.GAMES_EXPANSIONS, Games.GAME_ID + "=?", gameArg);
				db.delete(
						Tables.GAME_POLL_RESULTS_RESULT,
						"pollresults_id IN (SELECT game_poll_results._id from game_poll_results WHERE game_poll_results.poll_id IN (SELECT game_polls._id FROM game_polls WHERE game_id=?))",
						gameArg);
				db.delete(Tables.GAME_POLL_RESULTS,
						"game_poll_results.poll_id IN (SELECT game_polls._id FROM game_polls WHERE game_id=?)", gameArg);
				db.delete(Tables.GAME_POLLS, GamePolls.GAME_ID + "=?", gameArg);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}
}
