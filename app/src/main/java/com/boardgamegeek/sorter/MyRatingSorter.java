package com.boardgamegeek.sorter;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class MyRatingSorter extends CollectionSorter {
	private static final String DEFAULT_VALUE = "?";
	private final DecimalFormat displayFormat = new DecimalFormat("0.0");

	public MyRatingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.RATING, true);
        descriptionId = R.string.menu_collection_sort_myrating;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_MY_RATING;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.RATING };
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getInfo(cursor, null);
	}

	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		return getInfo(cursor, displayFormat);
	}

	private String getInfo(@NonNull Cursor cursor, DecimalFormat format) {
		return getDoubleAsString(cursor, Collection.RATING, DEFAULT_VALUE, true, format);
	}
}
