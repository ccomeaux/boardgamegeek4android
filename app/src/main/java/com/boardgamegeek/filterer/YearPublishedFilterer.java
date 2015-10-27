package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.provider.BggContract.Games;

import java.util.Calendar;

public class YearPublishedFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 1970;
	public static final int MAX_RANGE = Calendar.getInstance().get(Calendar.YEAR) + 1;

	private int min;
	private int max;

	public YearPublishedFilterer() {
		setType(CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED);
	}

	public YearPublishedFilterer(Context context, String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		init(context);
	}

	public YearPublishedFilterer(Context context, int min, int max) {
		this.min = min;
		this.max = max;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String text;
		String minText = String.valueOf(min);
		String maxText = String.valueOf(max);

		if (min == MIN_RANGE && max == MAX_RANGE) {
			text = "ALL";
		} else if (min == MIN_RANGE) {
			text = maxText + "-";
		} else if (max == MAX_RANGE) {
			text = minText + "+";
		} else if (min == max) {
			text = maxText;
		} else {
			text = minText + "-" + maxText;
		}

		displayText(text);
	}

	private void setSelection() {
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);

		String selection;
		if (min == MIN_RANGE && max == MAX_RANGE) {
			selection = "";
		} else if (min == MIN_RANGE) {
			selection = Games.YEAR_PUBLISHED + "<=?";
			selectionArgs(maxValue);
		} else if (max == MAX_RANGE) {
			selection = Games.YEAR_PUBLISHED + ">=?";
			selectionArgs(minValue);
		} else if (min == max) {
			selection = Games.YEAR_PUBLISHED + "=?";
			selectionArgs(minValue);
		} else {
			selection = "(" + Games.YEAR_PUBLISHED + ">=? AND " + Games.YEAR_PUBLISHED + "<=?)";
			selectionArgs(minValue, maxValue);
		}
		selection(selection);
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max);
	}
}
