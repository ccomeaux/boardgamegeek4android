package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

import java.text.DecimalFormat;

public class MyRatingSorter extends CollectionSorter {
	private static final String DEFAULT_VALUE = "?";
	private final DecimalFormat displayFormat = new DecimalFormat("0.0");

	public MyRatingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.RATING, true);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_my_rating;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_my_rating;
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
