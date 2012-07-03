package com.boardgamegeek.data;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeSortData extends CollectionSortData {
	@Override
	public String[] getColumns() {
		return new String[] { Collection.PLAYING_TIME };
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getScrollText(cursor) + " mins";
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getIntAsString(cursor, Collection.PLAYING_TIME, "?");
	}
}
