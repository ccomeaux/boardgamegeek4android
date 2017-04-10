package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;

public class YearPublishedDescendingSorter extends YearPublishedSorter {
	public YearPublishedDescendingSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_year_published_desc;
	}

	@Override
	protected boolean isSortDescending() {
		return true;
	}

	@StringRes
	@Override
	public int getSubDescriptionId() {
		return R.string.newest;
	}
}
