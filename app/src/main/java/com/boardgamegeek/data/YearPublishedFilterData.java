package com.boardgamegeek.data;

import java.util.Calendar;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.provider.BggContract.Games;

public class YearPublishedFilterData extends CollectionFilterData {
	public static final int MIN_RANGE = 1970;
	public static final int MAX_RANGE = Calendar.getInstance().get(Calendar.YEAR) + 1;

	private static final String delimiter = ":";

	private int mMin;
	private int mMax;

	public YearPublishedFilterData() {
		setType(CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED);
	}

	public YearPublishedFilterData(Context context, String data) {
		String[] d = data.split(delimiter);
		mMin = Integer.valueOf(d[0]);
		mMax = Integer.valueOf(d[1]);
		init(context);
	}

	public YearPublishedFilterData(Context context, int min, int max) {
		mMin = min;
		mMax = max;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String text = "";
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		if (mMin == MIN_RANGE && mMax == MAX_RANGE) {
			text = "ALL";
		} else if (mMin == MIN_RANGE) {
			text = maxValue + "-";
		} else if (mMax == MAX_RANGE) {
			text = minValue + "+";
		} else if (mMin == mMax) {
			text = maxValue;
		} else {
			text = minValue + "-" + maxValue;
		}

		displayText(text);
	}

	private void setSelection() {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		String selection = "";
		if (mMin == MIN_RANGE && mMax == MAX_RANGE) {
			// leave blank to cause it to clear
		} else if (mMin == MIN_RANGE) {
			selection = Games.YEAR_PUBLISHED + "<=?";
			selectionArgs(maxValue);
		} else if (mMax == MAX_RANGE) {
			selection = Games.YEAR_PUBLISHED + ">=?";
			selectionArgs(minValue);
		} else if (mMin == mMax) {
			selection = Games.YEAR_PUBLISHED + "=?";
			selectionArgs(minValue);
		} else {
			selection = "(" + Games.YEAR_PUBLISHED + ">=? AND " + Games.YEAR_PUBLISHED + "<=?)";
			selectionArgs(minValue, maxValue);
		}
		selection(selection);
	}

	public int getMin() {
		return mMin;
	}

	public int getMax() {
		return mMax;
	}

	@Override
	public String flatten() {
		return String.valueOf(mMin) + delimiter + String.valueOf(mMax);
	}
}
