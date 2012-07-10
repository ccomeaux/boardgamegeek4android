package com.boardgamegeek.data;

import java.text.DecimalFormat;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class AverageWeightSortData extends CollectionSortData {
	private static final String DEFAULT_VALUE = "?";

	public AverageWeightSortData(Context context) {
		super(context);
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.STATS_AVERAGE_WEIGHT };
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return mContext.getString(R.string.weight) + " "+ getInfo(cursor, new DecimalFormat("0.000"));
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getInfo(cursor, null);
	}

	private String getInfo(Cursor cursor, DecimalFormat format) {
		return getDoubleAsString(cursor, Collection.STATS_AVERAGE_WEIGHT, DEFAULT_VALUE, true, format);
	}
}
