package com.boardgamegeek.data;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeSortData extends CollectionSortData {
	public PlayTimeSortData(Context context) {
		super(context);
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.PLAYING_TIME };
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getScrollText(cursor) + " " + mContext.getString(R.string.time_suffix);
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getIntAsString(cursor, Collection.PLAYING_TIME, "?");
	}
}
