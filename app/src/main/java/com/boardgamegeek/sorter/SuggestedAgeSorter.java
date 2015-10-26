package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public abstract class SuggestedAgeSorter extends CollectionSorter {
	private static final String DEFAULT_VALUE = "?";

	public SuggestedAgeSorter(Context context) {
		super(context);
		descriptionId = R.string.menu_collection_sort_age;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.MINIMUM_AGE };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getIntAsString(cursor, Collection.MINIMUM_AGE, DEFAULT_VALUE, true);
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		String info = getHeaderText(cursor);
		if (!DEFAULT_VALUE.equals(info)) {
			info += "+";
		}
		return context.getString(R.string.ages) + " " + info;
	}
}
