package com.boardgamegeek.filterer;

import android.content.Context;
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

	public MyRatingFilterer(Context context) {
		super(context);
	}

	public MyRatingFilterer(@NonNull Context context, double min, double max, boolean includeUnrated) {
		super(context);
		this.min = min;
		this.max = max;
		this.includeUnrated = includeUnrated;
		init(context);
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Double.valueOf(d[0]);
		max = Double.valueOf(d[1]);
		includeUnrated = (d[2].equals("1"));
		init(context);
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_MY_RATING;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max) + DELIMITER + (includeUnrated ? "1" : "0");
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
		setSelection();
	}

	@Override
	public String getDisplayText() {
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

		return context.getString(R.string.my_rating) + " " + text;
	}

	private void setSelection() {
		String selection;
		if (min == max) {
			selection = Collection.RATING + "=?";
		} else {
			selection = "(" + Collection.RATING + ">=? AND " + Collection.RATING + "<=?)";
		}
		if (includeUnrated) {
			selection += " OR " + Collection.RATING + "=0 OR " + Collection.RATING + " IS NULL";
		}
		selection(selection);
	}

	@Override
	public String[] getSelectionArgs() {
		if (min == max) {
			return new String[] { String.valueOf(min) };
		} else {
			return new String[] { String.valueOf(min), String.valueOf(max) };
		}
	}
}
