package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.provider.BggContract.Plays;

public abstract class PlaysSorter extends Sorter {

	public PlaysSorter(Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return Plays.DEFAULT_SORT;
	}
}