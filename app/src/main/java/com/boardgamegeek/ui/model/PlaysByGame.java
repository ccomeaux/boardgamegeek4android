package com.boardgamegeek.ui.model;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysByGame {
	public static final String[] PROJECTION = {
		Plays._ID,
		Plays.MAX_DATE,
		Plays.SUM_QUANTITY
	};

	public static final Uri URI = Plays.CONTENT_URI;

	private static final int MAX_DATE = 1;
	private static final int SUM_QUANTITY = 2;

	private long maxDate;
	private int count;

	private PlaysByGame() {
	}

	public static PlaysByGame fromCursor(Cursor cursor) {
		PlaysByGame playsByGame = new PlaysByGame();
		playsByGame.maxDate = CursorUtils.getDateInMillis(cursor, MAX_DATE);
		playsByGame.count = cursor.getInt(SUM_QUANTITY);
		return playsByGame;
	}

	public static String getSelection(Context context) {
		String selection = String.format("%s=? AND %s", Plays.OBJECT_ID, SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP));
		if (!PreferencesUtils.logPlayStatsIncomplete(context)) {
			selection += String.format(" AND %s!=1", Plays.INCOMPLETE);
		}
		return selection;
	}

	public static String[] getSelectionArgs(int gameId) {
		return new String[] { String.valueOf(gameId) };
	}

	public long getMaxDateInMillis() {
		return maxDate;
	}

	public int getCount() {
		return count;
	}
}
