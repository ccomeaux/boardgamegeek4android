package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;

public class YearPublishedAscendingSorter extends YearPublishedSorter {
	public YearPublishedAscendingSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_year_published_asc;
	}

	@StringRes
	@Override
	public int getSubDescriptionId() {
		return R.string.oldest;
	}
}
