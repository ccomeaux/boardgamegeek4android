package com.boardgamegeek.data;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class GeekRatingSortData extends CollectionSortData {
	private DecimalFormat mScrollDf = new DecimalFormat("#.0");
	private DecimalFormat mDisplayDf = new DecimalFormat("#.###");

	public GeekRatingSortData(Context context) {
		super(context);
		mOrderByClause = Collection.SORT_BY_RATING;
		mDescriptionId = R.string.rating;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.STATS_BAYES_AVERAGE };
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		Double rating = getDouble(cursor, Collection.STATS_BAYES_AVERAGE);
		return (rating == null || rating == 0) ? null : mDisplayDf.format(rating);
	}

	@Override
	public String getScrollText(Cursor cursor) {
		Double rating = getDouble(cursor, Collection.STATS_BAYES_AVERAGE);
		return (rating == null) ? null : mScrollDf.format(rating);
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_GEEK_RATING;
	}
}
