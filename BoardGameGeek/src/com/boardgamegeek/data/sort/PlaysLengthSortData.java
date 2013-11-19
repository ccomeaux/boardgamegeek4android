package com.boardgamegeek.data.sort;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysLengthSortData extends PlaysSortData {
	private final String mNoLength;
	private final String mHourrsSuffix;
	private final String mMinutesSuffix;

	public PlaysLengthSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Plays.LENGTH, true);
		mDescriptionId = R.string.menu_plays_sort_length;
		mNoLength = context.getString(R.string.no_length);
		mHourrsSuffix = "+ " + mContext.getString(R.string.hours_abbr);
		mMinutesSuffix = "+ " + mContext.getString(R.string.minutes_abbr);
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
	public String getScrollText(Cursor cursor) {
		int minutes = getInt(cursor, Plays.LENGTH);
		if (minutes == 0) {
			return mNoLength;
		}
		if (minutes >= 120) {
			return (minutes / 60) + mHourrsSuffix;
		} else {
			return getScrollText(cursor) + mMinutesSuffix;
		}
	}
}
