package com.boardgamegeek.sorter;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class GeekRatingSorter extends CollectionSorter {
	private static final String DEFAULT_VALUE = "?";
	private DecimalFormat mDisplayFormat = new DecimalFormat("0.000");

	public GeekRatingSorter(Context context) {
		super(context);
		mOrderByClause = Collection.SORT_BY_RATING;
        mDescriptionId = R.string.menu_collection_sort_rating;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_GEEK_RATING;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.STATS_BAYES_AVERAGE };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getInfo(cursor, null);
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getInfo(cursor, mDisplayFormat);
	}

	private String getInfo(Cursor cursor, DecimalFormat format) {
		return getDoubleAsString(cursor, Collection.STATS_BAYES_AVERAGE, DEFAULT_VALUE, true, format);
	}
}
