package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class AverageWeightAscendingSorter extends AverageWeightSorter {
	public AverageWeightAscendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.STATS_AVERAGE_WEIGHT, false);
		subDescriptionId = R.string.lightest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_AVERAGE_WEIGHT_ASC;
	}
}
