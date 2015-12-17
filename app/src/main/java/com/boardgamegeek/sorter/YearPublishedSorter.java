package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public abstract class YearPublishedSorter extends CollectionSorter {
	public YearPublishedSorter(@NonNull Context context) {
		super(context);
		descriptionId = R.string.collection_sort_year_published;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.YEAR_PUBLISHED };
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getIntAsString(cursor, Collection.YEAR_PUBLISHED, "?");
	}
}
