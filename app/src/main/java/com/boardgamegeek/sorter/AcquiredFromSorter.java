package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class AcquiredFromSorter extends CollectionSorter {
	private static final String COLUMN_NAME = Collection.PRIVATE_INFO_ACQUIRED_FROM;
	@NonNull private final String nowhere;

	public AcquiredFromSorter(@NonNull Context context) {
		super(context);
		nowhere = context.getString(R.string.nowhere_in_angle_brackets);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_acquired_from;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_acquired_from;
	}

	@Override
	protected String getSortColumn() {
		return COLUMN_NAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { COLUMN_NAME };
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getString(cursor, COLUMN_NAME, nowhere);
	}
}
