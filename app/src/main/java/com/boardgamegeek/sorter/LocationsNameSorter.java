package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class LocationsNameSorter extends LocationsSorter {

	public LocationsNameSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Plays.LOCATION, false);
		mDescriptionId = R.string.menu_sort_name;
	}

	@Override
	public int getType() {
		return LocationsSorterFactory.TYPE_NAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.LOCATION };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getFirstChar(cursor, Plays.LOCATION);
	}
}
