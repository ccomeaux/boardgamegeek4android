package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class SuggestedAgeDescendingSorter extends SuggestedAgeSorter {
	public SuggestedAgeDescendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.MINIMUM_AGE, true);
		subDescriptionId = R.string.oldest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_AGE_DESC;
	}
}
