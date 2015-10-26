package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysLengthSorter extends PlaysSorter {
	private static final String AND_MORE_SUFFIX = "+ ";
	private final String noLength;
	private final String hoursSuffix;
	private final String minutesSuffix;

	public PlaysLengthSorter(Context context) {
		super(context);
		orderByClause = getClause(Plays.LENGTH, true);
		descriptionId = R.string.menu_plays_sort_length;
		noLength = context.getString(R.string.no_length);
		hoursSuffix = context.getString(R.string.hours_abbr);
		minutesSuffix = context.getString(R.string.minutes_abbr);
	}

	@Override
	public int getType() {
		return PlaysSorterFactory.TYPE_PLAY_LENGTH;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.LENGTH };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		int minutes = getInt(cursor, Plays.LENGTH);
		if (minutes == 0) {
			return noLength;
		}
		if (minutes >= 120) {
			return (minutes / 60) + AND_MORE_SUFFIX + hoursSuffix;
		} else if (minutes >= 60) {
			return (minutes / 10) * 10 + AND_MORE_SUFFIX + minutesSuffix;
		} else if (minutes >= 30) {
			return (minutes / 5) * 5 + AND_MORE_SUFFIX + minutesSuffix;
		} else {
			return minutes + minutesSuffix;
		}
	}
}
