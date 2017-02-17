package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class LocationsNameSorter extends LocationsSorter {
	public LocationsNameSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.menu_sort_name;
	}

	@Override
	public int getType() {
		return LocationsSorterFactory.TYPE_NAME;
	}

	@Override
	protected String getSortColumn() {
		return Plays.LOCATION;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.LOCATION };
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getFirstChar(cursor, Plays.LOCATION);
	}
}
