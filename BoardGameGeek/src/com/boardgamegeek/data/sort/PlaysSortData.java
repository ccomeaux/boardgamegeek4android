package com.boardgamegeek.data.sort;

import android.content.Context;

import com.boardgamegeek.provider.BggContract.Plays;

public abstract class PlaysSortData extends SortData {

	public PlaysSortData(Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return Plays.DEFAULT_SORT;
	}
}