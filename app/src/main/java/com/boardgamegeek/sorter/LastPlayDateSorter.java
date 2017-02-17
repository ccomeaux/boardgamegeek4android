package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class LastPlayDateSorter extends CollectionDateSorter {
	public LastPlayDateSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_play_date_max;
	}

	@Override
	protected String getSortColumn() {
		return Plays.MAX_DATE;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_play_date_max;
	}
}
