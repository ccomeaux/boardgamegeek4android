package com.boardgamegeek.data;

import java.text.DecimalFormat;

import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class CollectionNameSortData extends CollectionSortData {
	private DecimalFormat mDisplayDf = new DecimalFormat("#.###");

	public CollectionNameSortData() {
		mOrderByClause = Collection.DEFAULT_SORT;
		mDescription = R.string.name;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.COLLECTION_SORT_NAME, Collection.STATS_BAYES_AVERAGE };
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		Double rating = getDouble(cursor, Collection.STATS_BAYES_AVERAGE);
		return (rating == null || rating == 0) ? null : mDisplayDf.format(rating);
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getFirstChar(cursor, Collection.COLLECTION_SORT_NAME);
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_COLLECTION_NAME;
	}
}
