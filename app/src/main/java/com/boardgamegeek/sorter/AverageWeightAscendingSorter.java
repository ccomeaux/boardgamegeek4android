package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class AverageWeightAscendingSorter extends AverageWeightSorter {
	public AverageWeightAscendingSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.STATS_AVERAGE_WEIGHT, false);
		mSubDescriptionId = R.string.lightest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_AVERAGE_WEIGHT_ASC;
	}
}
