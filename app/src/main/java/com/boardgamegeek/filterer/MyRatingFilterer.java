package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class MyRatingFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 0.0;
	public static final double MAX_RANGE = 10.0;
	private static final String DELIMITER = ":";

	private double min;
	private double max;

	public MyRatingFilterer() {
		setType(CollectionFilterDataFactory.TYPE_MY_RATING);
	}

	public MyRatingFilterer(Context context, double min, double max) {
		this.min = min;
		this.max = max;
		init(context);
	}

	public MyRatingFilterer(Context context, String data) {
		String[] d = data.split(DELIMITER);
		min = Double.valueOf(d[0]);
		max = Double.valueOf(d[1]);
		init(context);
	}

	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max);
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_MY_RATING);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String minText = String.valueOf(min);
		String maxText = String.valueOf(max);

		String text;
		if (min == max) {
			text = maxText;
		} else {
			text = minText + "-" + maxText;
		}
		displayText(r.getString(R.string.my_rating) + " " + text);
	}

	private void setSelection() {
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);

		String selection;
		if (min == max) {
			selection = Collection.RATING + "=?";
			selectionArgs(minValue);
		} else {
			selection = "(" + Collection.RATING + ">=? AND " + Collection.RATING + "<=?)";
			selectionArgs(minValue, maxValue);
		}
		selection(selection);
	}
}
