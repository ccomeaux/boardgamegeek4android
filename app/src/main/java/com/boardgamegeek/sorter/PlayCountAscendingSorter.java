package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountAscendingSorter extends PlayCountSorter {
	public PlayCountAscendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.NUM_PLAYS, false);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_play_count_asc;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_play_count_asc;
	}
}
