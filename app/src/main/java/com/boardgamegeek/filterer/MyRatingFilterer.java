package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class MyRatingFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 1.0;
	public static final double MAX_RANGE = 10.0;

	protected Context context;
	private double min;
	private double max;
	private boolean includeUnrated;

	public MyRatingFilterer() {
		setType(CollectionFilterDataFactory.TYPE_MY_RATING);
	}

	public MyRatingFilterer(@NonNull Context context, double min, double max, boolean includeUnrated) {
		this.min = min;
		this.max = max;
		this.includeUnrated = includeUnrated;
		init(context);
	}

	public MyRatingFilterer(@NonNull Context context, @NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Double.valueOf(d[0]);
		max = Double.valueOf(d[1]);
		includeUnrated = (d[2].equals("1"));
		init(context);
	}

	@NonNull
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

	public boolean includeUnrated() {
		return includeUnrated;
	}

	private void init(@NonNull Context context) {
		this.context = context;
		setType(CollectionFilterDataFactory.TYPE_MY_RATING);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(@NonNull Resources r) {
		String minText = String.valueOf(min);
		String maxText = String.valueOf(max);

		String text;
		if (min == max) {
			text = maxText;
		} else {
			text = minText + "-" + maxText;
		}
		if (includeUnrated) {
			text += " (+" + context.getString(R.string.unrated) + ")";
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
		if (includeUnrated) {
			selection += " OR " + Collection.RATING + "=0 OR " + Collection.RATING + " IS NULL";
		}
		selection(selection);
	}
}
