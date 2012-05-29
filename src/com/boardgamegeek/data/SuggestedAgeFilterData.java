package com.boardgamegeek.data;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class SuggestedAgeFilterData extends CollectionFilterData {
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 21;

	private static final String delimiter = ":";

	private int mMin;
	private int mMax;
	private boolean mUndefined;

	public SuggestedAgeFilterData() {
	}

	public SuggestedAgeFilterData(Context context, String data) {
		String[] d = data.split(delimiter);
		mMin = Integer.valueOf(d[0]);
		mMax = Integer.valueOf(d[1]);
		mUndefined = (d[2].equals("1"));
		init(context);
	}

	public SuggestedAgeFilterData(Context context, int min, int max, boolean undefined) {
		mMin = min;
		mMax = max;
		mUndefined = undefined;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_SUGGESTED_AGE);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String text = "";
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		if (mMax == MAX_RANGE) {
			text = minValue + "+";
		} else if (mMin == mMax) {
			text = maxValue;
		} else {
			text = minValue + "-" + maxValue;
		}
		if (mUndefined) {
			text += " (+?)";
		}
		displayText(r.getString(R.string.ages) + " " + text);
	}

	private void setSelection() {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		String selection = "";
		if (mMax == MAX_RANGE) {
			selection = "(" + Games.MINIMUM_AGE + ">=?)";
			selectionArgs(minValue);
		} else {
			selection = "(" + Games.MINIMUM_AGE + ">=? AND " + Games.MINIMUM_AGE + "<=?)";
			selectionArgs(minValue, maxValue);
		}
		if (mUndefined) {
			selection += " OR " + Games.MINIMUM_AGE + "=0 OR " + Games.MINIMUM_AGE + " IS NULL";
		}
		selection(selection);
	}

	public int getMin() {
		return mMin;
	}

	public int getMax() {
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
