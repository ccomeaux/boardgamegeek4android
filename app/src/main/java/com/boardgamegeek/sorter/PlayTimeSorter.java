package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeSorter extends CollectionSorter {
	public PlayTimeSorter(Context context) {
		super(context);
		mDescriptionId = R.string.menu_collection_sort_playtime;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.PLAYING_TIME };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		int minutes = getInt(cursor, Collection.PLAYING_TIME);
		if (minutes == 0) {
			return "?";
		}
		if (minutes >= 120) {
			return (minutes / 60) + " " + mContext.getString(R.string.hours_abbr);
		} else {
			return getIntAsString(cursor, Collection.PLAYING_TIME, "?") + " "
				+ mContext.getString(R.string.minutes_abbr);
		}
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getIntAsString(cursor, Collection.PLAYING_TIME, "?") + " " + mContext.getString(R.string.minutes);
	}
}
