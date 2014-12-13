package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public abstract class YearPublishedSorter extends CollectionSorter {
	public YearPublishedSorter(Context context) {
		super(context);
		mDescriptionId = R.string.menu_collection_sort_published;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.YEAR_PUBLISHED };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getIntAsString(cursor, Collection.YEAR_PUBLISHED, "?");
	}
}
