package com.boardgamegeek.data;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class SuggestedAgeSortData extends CollectionSortData {
	private static final String DEFAULT_VALUE = "?";

	public SuggestedAgeSortData(Context context) {
		super(context);
		mDescriptionId = R.string.menu_collection_sort_age;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.MINIMUM_AGE };
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getIntAsString(cursor, Collection.MINIMUM_AGE, DEFAULT_VALUE, true);
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		String info = getScrollText(cursor);
		if (!DEFAULT_VALUE.equals(info)) {
			info += "+";
		}
		return mContext.getString(R.string.ages) + " " + info;
	}
}
