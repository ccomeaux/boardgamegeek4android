package com.boardgamegeek.data;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

import android.content.Context;
import android.content.res.Resources;

public class AverageRatingFilterData extends CollectionFilterData {
	public static final double MIN_RANGE = 0.0;
	public static final double MAX_RANGE = 10.0;

	private static final String delimiter = ":";

	private double mMin;
	private double mMax;

	public AverageRatingFilterData() {
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_RATING);
	}

	public AverageRatingFilterData(Context context, String data) {
		String[] d = data.split(delimiter);
		mMin = Double.valueOf(d[0]);
		mMax = Double.valueOf(d[1]);
		init(context);
	}

	public AverageRatingFilterData(Context context, double min, double max) {
		mMin = min;
		mMax = max;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_RATING);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		String text = "";
		if (mMin == mMax) {
			text = maxValue;
		} else {
			text = minValue + "-" + maxValue;
		}
		displayText(r.getString(R.string.average) + " " + text);
	}

	private void setSelection() {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		String selection = "";
		if (mMin == mMax) {
			selection = Games.STATS_AVERAGE + "=?";
			selectionArgs(minValue);
		} else {
			selection = "(" + Games.STATS_AVERAGE + ">=? AND " + Games.STATS_AVERAGE + "<=?)";
			selectionArgs(minValue, maxValue);
		}
		selection(selection);
	}

	public double getMin() {
		return mMin;
	}

	public double getMax() {
		return mMax;
	}

	@Override
	public String flatten() {
		return String.valueOf(mMin) + delimiter + String.valueOf(mMax);
	}
}
