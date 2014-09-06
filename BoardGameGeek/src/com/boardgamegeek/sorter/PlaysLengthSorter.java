package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysLengthSorter extends PlaysSorter {
	private static final String AND_MORE_SUFFIX = "+ ";
	private final String mNoLength;
	private final String mHoursSuffix;
	private final String mMinutesSuffix;

	public PlaysLengthSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Plays.LENGTH, true);
		mDescriptionId = R.string.menu_plays_sort_length;
		mNoLength = context.getString(R.string.no_length);
		mHoursSuffix = mContext.getString(R.string.hours_abbr);
		mMinutesSuffix = mContext.getString(R.string.minutes_abbr);
	}

	@Override
	public int getType() {
		return PlaysSortDataFactory.TYPE_PLAY_LENGTH;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.LENGTH };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		int minutes = getInt(cursor, Plays.LENGTH);
		if (minutes == 0) {
			return mNoLength;
		}
		if (minutes >= 120) {
			return (minutes / 60) + AND_MORE_SUFFIX + mHoursSuffix;
		} else if (minutes >= 60) {
			return (minutes / 10) * 10 + AND_MORE_SUFFIX + mMinutesSuffix;
		} else if (minutes >= 30) {
			return (minutes / 5) * 5 + AND_MORE_SUFFIX + mMinutesSuffix;
		} else {
			return minutes + mMinutesSuffix;
		}
	}
}
