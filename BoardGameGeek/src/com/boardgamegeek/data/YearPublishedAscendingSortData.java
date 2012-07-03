package com.boardgamegeek.data;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class YearPublishedAscendingSortData extends YearPublishedSortData {
	public YearPublishedAscendingSortData() {
		mOrderByClause = getClause(Collection.YEAR_PUBLISHED, false);
		mDescription = R.string.menu_collection_sort_published_oldest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_YEAR_PUBLISHED_ASC;
	}
}
