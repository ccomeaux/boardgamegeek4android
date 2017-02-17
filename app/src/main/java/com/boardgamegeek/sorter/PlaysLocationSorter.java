package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysLocationSorter extends PlaysSorter {
	@NonNull private final String noLocation;

	public PlaysLocationSorter(@NonNull Context context) {
		super(context);
		noLocation = context.getString(R.string.no_location);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.menu_plays_sort_location;
	}

	@Override
	public int getType() {
		return PlaysSorterFactory.TYPE_PLAY_LOCATION;
	}

	@Override
	protected String getSortColumn() {
		return Plays.LOCATION;
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getString(cursor, Plays.LOCATION, noLocation);
	}
}
