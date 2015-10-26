package com.boardgamegeek.sorter;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class GeekRatingSorter extends CollectionSorter {
	private static final String COLUMN = Collection.STATS_BAYES_AVERAGE;
	private static final String DEFAULT_VALUE = "?";
	private final DecimalFormat displayFormat = new DecimalFormat("0.000");

	public GeekRatingSorter(Context context) {
		super(context);
		orderByClause = getClause(COLUMN, true);
		descriptionId = R.string.menu_collection_sort_geek_rating;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_GEEK_RATING;
	}

	@Override
	public String[] getColumns() {
		return new String[] { COLUMN };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getInfo(cursor, null);
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getInfo(cursor, displayFormat);
	}

	private String getInfo(Cursor cursor, DecimalFormat format) {
		return getDoubleAsString(cursor, COLUMN, DEFAULT_VALUE, true, format);
	}
}
