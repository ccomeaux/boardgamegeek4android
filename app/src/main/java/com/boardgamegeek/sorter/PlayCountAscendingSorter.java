package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;

public class PlayCountAscendingSorter extends PlayCountSorter {
	public PlayCountAscendingSorter(@NonNull Context context) {
		super(context);
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
