package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.PLAY_ITEMS_JOIN_PLAYS).mapToTable(BaseColumns._ID, Tables.PLAYS)
				.mapToTable(Plays.PLAY_ID, Tables.PLAYS);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.PLAYS);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Plays.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "plays";
	}

	@Override
	protected String getType(Uri uri) {
		return Plays.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		if (db.insertOrThrow(Tables.PLAYS, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Plays.buildPlayUri(values.getAsInteger(Plays.PLAY_ID));
	}

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		// TODO after upgrading to API 8, use cascading deletes (http://stackoverflow.com/questions/2545558)
		Cursor cursor = builder.query(db, new String[] { Plays.PLAY_ID }, null);
		try {
			while (cursor.moveToNext()) {
				int playId = cursor.getInt(0);
				String[] playArg = new String[] { String.valueOf(playId) };
				db.delete(Tables.PLAY_ITEMS, PlayItems.PLAY_ID + "=?", playArg);
				db.delete(Tables.PLAY_PLAYERS, PlayItems.PLAY_ID + "=?", playArg);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}
}
