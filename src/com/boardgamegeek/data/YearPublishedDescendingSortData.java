package com.boardgamegeek.data;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class YearPublishedDescendingSortData extends YearPublishedSortData {
	public YearPublishedDescendingSortData() {
		mOrderByClause = getClause(Collection.YEAR_PUBLISHED, true);
		mDescription = R.string.menu_collection_sort_published_newest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_YEAR_PUBLISHED_DESC;
	}
}
