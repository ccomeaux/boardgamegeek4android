package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

import java.text.DecimalFormat;

public class CollectionNameSorter extends CollectionSorter {
	@NonNull private final DecimalFormat displayFormat = new DecimalFormat("0.00");

	public CollectionNameSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_collection_name;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_collection_name;
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
