package com.boardgamegeek.data;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class CollectionNameSortData extends CollectionSortData {
	private DecimalFormat mDisplayFormat = new DecimalFormat("0.000");

	public CollectionNameSortData(Context context) {
		super(context);
		mOrderByClause = Collection.DEFAULT_SORT;
		mDescriptionId = R.string.name;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_COLLECTION_NAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.COLLECTION_SORT_NAME, Collection.STATS_BAYES_AVERAGE };
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getFirstChar(cursor, Collection.COLLECTION_SORT_NAME);
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getDoubleAsString(cursor, Collection.STATS_BAYES_AVERAGE, "?", true, mDisplayFormat);
	}
}
