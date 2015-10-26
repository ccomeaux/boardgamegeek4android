package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysLocationSorter extends PlaysSorter {
	private final String noLocation;

	public PlaysLocationSorter(Context context) {
		super(context);
		orderByClause = getClause(Plays.LOCATION, false);
		descriptionId = R.string.menu_plays_sort_location;
		noLocation = context.getString(R.string.no_location);
	}

	@Override
	public int getType() {
		return PlaysSorterFactory.TYPE_PLAY_LOCATION;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.LOCATION };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getString(cursor, Plays.LOCATION, noLocation);
	}
}
