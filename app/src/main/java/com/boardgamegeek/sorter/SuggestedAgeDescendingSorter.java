package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class SuggestedAgeDescendingSorter extends SuggestedAgeSorter {
	public SuggestedAgeDescendingSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.MINIMUM_AGE, true);
		mSubDescriptionId = R.string.oldest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_AGE_DESC;
	}
}
