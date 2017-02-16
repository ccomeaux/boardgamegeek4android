package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class YearPublishedDescendingSorter extends YearPublishedSorter {
	public YearPublishedDescendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.YEAR_PUBLISHED, true);
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_year_published_desc;
	}

	@StringRes
	@Override
	public int getSubDescriptionId() {
		return R.string.newest;
	}
}
