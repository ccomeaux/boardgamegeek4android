package com.boardgamegeek.data.sort;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class SuggestedAgeDescendingSortData extends SuggestedAgeSortData {
	public SuggestedAgeDescendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.MINIMUM_AGE, true);
		mSubDescriptionId = R.string.oldest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_AGE_DESC;
	}
}
