package com.boardgamegeek.data.sort;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysLocationSortData extends PlaysSortData {
	private final String mNoLocation;

	public PlaysLocationSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Plays.LOCATION, false);
		mDescriptionId = R.string.menu_plays_sort_location;
		mNoLocation = "<" + context.getString(R.string.no_location) + ">";
	}

	@Override
	public int getType() {
		return PlaysSortDataFactory.TYPE_PLAY_LOCATION;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.LOCATION };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getString(cursor, Plays.LOCATION, mNoLocation);
	}
}
