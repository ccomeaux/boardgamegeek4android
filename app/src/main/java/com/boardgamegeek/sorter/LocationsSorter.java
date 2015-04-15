package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.provider.BggContract.Plays;

public abstract class LocationsSorter extends Sorter {

	public LocationsSorter(Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return Plays.LOCATION;
	}
}