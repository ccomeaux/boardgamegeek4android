package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public abstract class YearPublishedSorter extends CollectionSorter {
	public YearPublishedSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_year_published;
	}

	@Override
	protected String getSortColumn() {
		return Collection.YEAR_PUBLISHED;
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getIntAsString(cursor, Collection.YEAR_PUBLISHED, "?");
	}
}
