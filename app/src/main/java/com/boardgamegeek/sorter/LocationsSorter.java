package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Plays;

public abstract class LocationsSorter extends Sorter {

	public LocationsSorter(@NonNull Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return Plays.LOCATION;
	}
}