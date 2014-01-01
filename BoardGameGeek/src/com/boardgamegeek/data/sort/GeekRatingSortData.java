package com.boardgamegeek.data.sort;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class GeekRatingSortData extends CollectionSortData {
	private static final String DEFAULT_VALUE = "?";
	private DecimalFormat mDisplayFormat = new DecimalFormat("0.000");

	public GeekRatingSortData(Context context) {
		super(context);
		mOrderByClause = Collection.SORT_BY_RATING;
		mDescriptionId = R.string.rating;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_GEEK_RATING;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.STATS_BAYES_AVERAGE };
	}

	@Override
	public String getScrollText(Cursor cursor) {
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
