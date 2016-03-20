package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class YearPublishedAscendingSorter extends YearPublishedSorter {
	public YearPublishedAscendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.YEAR_PUBLISHED, false);
		subDescriptionId = R.string.oldest;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_year_published_asc;
	}
}
