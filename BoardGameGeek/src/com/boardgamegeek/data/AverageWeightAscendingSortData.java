package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class AverageWeightAscendingSortData extends AverageWeightSortData {
	public AverageWeightAscendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.STATS_AVERAGE_WEIGHT, false);
		mSubDescriptionId = R.string.lightest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_AVERAGE_WEIGHT_ASC;
	}
}
