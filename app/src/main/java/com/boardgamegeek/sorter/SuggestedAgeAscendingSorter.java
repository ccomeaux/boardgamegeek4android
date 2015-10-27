package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class SuggestedAgeAscendingSorter extends SuggestedAgeSorter {
	public SuggestedAgeAscendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.MINIMUM_AGE, false);
		subDescriptionId = R.string.youngest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_AGE_ASC;
	}
}
