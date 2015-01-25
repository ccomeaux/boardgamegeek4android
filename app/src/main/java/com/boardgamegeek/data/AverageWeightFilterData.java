package com.boardgamegeek.data;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class AverageWeightFilterData extends CollectionFilterData {
	public static final double MIN_RANGE = 1.0;
	public static final double MAX_RANGE = 5.0;

	private static final String delimiter = ":";

	private double mMin;
	private double mMax;
	private boolean mUndefined;

	public AverageWeightFilterData() {
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT);
	}

	public AverageWeightFilterData(Context context, String data) {
		String[] d = data.split(delimiter);
		mMin = Double.valueOf(d[0]);
		mMax = Double.valueOf(d[1]);
		mUndefined = (d[2].equals("1"));
		init(context);
	}

	public AverageWeightFilterData(Context context, double min, double max, boolean undefined) {
		mMin = min;
		mMax = max;
		mUndefined = undefined;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		String text;
		if (mMin == mMax) {
			text = maxValue;
		} else {
			text = minValue + "-" + maxValue;
		}
		if (mUndefined) {
			text += " (+?)";
		}

		displayText(r.getString(R.string.weight) + " " + text);
	}

	private void setSelection() {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		String selection;
		selection = "(" + Games.STATS_AVERAGE_WEIGHT + ">=? AND " + Games.STATS_AVERAGE_WEIGHT + "<=?)";
		selectionArgs(minValue, maxValue);
		if (mUndefined) {
			selection += " OR " + Games.STATS_AVERAGE_WEIGHT + "=0 OR " + Games.STATS_AVERAGE_WEIGHT + " IS NULL";
		}
		selection(selection);
	}

	public double getMin() {
		return mMin;
	}

	public double getMax() {
		return mMax;
	}

	public boolean isUndefined() {
		return mUndefined;
	}

	@Override
	public String flatten() {
		return String.valueOf(mMin) + delimiter + String.valueOf(mMax) + delimiter + (mUndefined ? "1" : "0");
	}
}
