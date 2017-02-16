package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

import java.text.DecimalFormat;

public abstract class AverageWeightSorter extends CollectionSorter {
	private static final String DEFAULT_VALUE = "?";
	private final DecimalFormat displayFormat = new DecimalFormat("0.000");

	public AverageWeightSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_average_weight;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.STATS_AVERAGE_WEIGHT };
	}

	@NonNull
	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		return context.getString(R.string.weight) + " " + getInfo(cursor, displayFormat);
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getInfo(cursor, null);
	}

	private String getInfo(@NonNull Cursor cursor, DecimalFormat format) {
		return getDoubleAsString(cursor, Collection.STATS_AVERAGE_WEIGHT, DEFAULT_VALUE, true, format);
	}
}
