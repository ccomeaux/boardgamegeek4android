package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class SuggestedAgeAscendingSortData extends SuggestedAgeSortData {
	public SuggestedAgeAscendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.MINIMUM_AGE, false);
		mDescriptionId = R.string.menu_collection_sort_age_youngest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_AGE_ASC;
	}
}
