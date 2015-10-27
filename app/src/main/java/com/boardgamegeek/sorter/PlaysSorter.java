package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Plays;

public abstract class PlaysSorter extends Sorter {

	public PlaysSorter(@NonNull Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return Plays.DEFAULT_SORT;
	}
}