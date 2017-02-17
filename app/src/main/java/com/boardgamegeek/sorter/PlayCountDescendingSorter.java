package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;

public class PlayCountDescendingSorter extends PlayCountSorter {
	public PlayCountDescendingSorter(@NonNull Context context) {
		super(context);
	}

	@Override
	protected boolean isSortDescending() {
		return true;
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_play_count_desc;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_play_count_desc;
	}
}
