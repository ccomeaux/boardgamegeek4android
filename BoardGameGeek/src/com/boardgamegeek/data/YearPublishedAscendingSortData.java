package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class YearPublishedAscendingSortData extends YearPublishedSortData {
	public YearPublishedAscendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.YEAR_PUBLISHED, false);
		mDescriptionId = R.string.menu_collection_sort_published_oldest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_YEAR_PUBLISHED_ASC;
	}
}
