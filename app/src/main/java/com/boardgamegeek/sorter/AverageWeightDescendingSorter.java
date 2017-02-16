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
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_average_weight_desc;
	}

	@StringRes
	@Override
	public int getSubDescriptionId() {
		return R.string.heaviest;
	}
}
