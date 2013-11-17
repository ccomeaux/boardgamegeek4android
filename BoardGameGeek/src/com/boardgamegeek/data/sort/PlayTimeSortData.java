package com.boardgamegeek.data.sort;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeSortData extends CollectionSortData {
	public PlayTimeSortData(Context context) {
		super(context);
		mDescriptionId = R.string.menu_collection_sort_playtime;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.PLAYING_TIME };
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getIntAsString(cursor, Collection.PLAYING_TIME, "?");
	}

	@Override
	public String getSectionText(Cursor cursor) {
		int minutes = getInt(cursor, Collection.PLAYING_TIME);
		if (minutes == 0) {
			return "?";
		}
		if (minutes >= 120) {
			return (minutes / 60) + " " + mContext.getString(R.string.hours_abbr);
		} else {
			return getScrollText(cursor) + " " + mContext.getString(R.string.minutes_abbr);
		}
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getScrollText(cursor) + " " + mContext.getString(R.string.minutes);
	}
}
