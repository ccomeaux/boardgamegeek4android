package com.boardgamegeek.data.sort;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class AverageWeightDescendingSortData extends AverageWeightSortData {
	public AverageWeightDescendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.STATS_AVERAGE_WEIGHT, true);
		mSubDescriptionId = R.string.heaviest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_AVERAGE_WEIGHT_DESC;
	}
}
