package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class AverageWeightDescendingSorter extends AverageWeightSorter {
	public AverageWeightDescendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.STATS_AVERAGE_WEIGHT, true);
		subDescriptionId = R.string.heaviest;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_average_weight_desc;
	}
}
