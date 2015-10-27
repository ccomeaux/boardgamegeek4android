package com.boardgamegeek.sorter;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class CollectionNameSorter extends CollectionSorter {
	@NonNull private final DecimalFormat displayFormat = new DecimalFormat("0.00");

	public CollectionNameSorter(@NonNull Context context) {
		super(context);
		orderByClause = Collection.DEFAULT_SORT;
		descriptionId = R.string.name;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_COLLECTION_NAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.COLLECTION_SORT_NAME, Collection.STATS_AVERAGE };
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getFirstChar(cursor, Collection.COLLECTION_SORT_NAME);
	}

	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		return getDoubleAsString(cursor, Collection.STATS_AVERAGE, "?", true, displayFormat);
	}
}
