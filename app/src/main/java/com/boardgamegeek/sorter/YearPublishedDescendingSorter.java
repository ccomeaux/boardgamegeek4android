package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class YearPublishedDescendingSorter extends YearPublishedSorter {
	public YearPublishedDescendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.YEAR_PUBLISHED, true);
		subDescriptionId = R.string.newest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_YEAR_PUBLISHED_DESC;
	}
}
