package com.boardgamegeek.data;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Collection;

public abstract class YearPublishedSortData extends CollectionSortData {
	public YearPublishedSortData(Context context) {
		super(context);
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.YEAR_PUBLISHED };
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getScrollText(cursor);
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getIntAsString(cursor, Collection.YEAR_PUBLISHED, "?");
	}
}
