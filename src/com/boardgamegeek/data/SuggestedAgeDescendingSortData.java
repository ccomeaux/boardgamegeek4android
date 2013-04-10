package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class SuggestedAgeDescendingSortData extends SuggestedAgeSortData {
	public SuggestedAgeDescendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.MINIMUM_AGE, true);
		mDescriptionId = R.string.menu_collection_sort_age_oldest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_AGE_DESC;
	}
}
