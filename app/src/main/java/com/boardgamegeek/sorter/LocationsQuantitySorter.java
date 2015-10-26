package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class LocationsQuantitySorter extends LocationsSorter {
	public LocationsQuantitySorter(Context context) {
		super(context);
		orderByClause = getClause(Plays.SUM_QUANTITY, true);
		descriptionId = R.string.menu_sort_quantity;
	}

	@Override
	public int getType() {
		return LocationsSorterFactory.TYPE_QUANTITY;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.SUM_QUANTITY };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		int q = getInt(cursor, Plays.SUM_QUANTITY);
		String prefix = String.valueOf(q).substring(0, 1);
		String suffix = "";
		if (q >= 10000) {
			suffix = "0000+";
		} else if (q >= 1000) {
			suffix = "000+";
		} else if (q >= 100) {
			suffix = "00+";
		} else if (q >= 10) {
			suffix = "0+";
		}
		return prefix + suffix;
	}
}
