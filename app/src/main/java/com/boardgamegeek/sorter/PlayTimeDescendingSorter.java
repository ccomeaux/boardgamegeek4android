package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;

public class PlayTimeDescendingSorter extends PlayTimeSorter {
	public PlayTimeDescendingSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_play_time_desc;
	}

	@Override
	protected boolean isSortDescending() {
		return true;
	}

	@StringRes
	@Override
	public int getSubDescriptionId() {
		return R.string.longest;
	}
}
