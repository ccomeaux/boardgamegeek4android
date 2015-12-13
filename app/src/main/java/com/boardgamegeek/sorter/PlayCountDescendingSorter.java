package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountDescendingSorter extends PlayCountSorter {
	public PlayCountDescendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.NUM_PLAYS, true);
		descriptionId = R.string.menu_collection_sort_played_most;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_average_weight_desc;
	}
}
