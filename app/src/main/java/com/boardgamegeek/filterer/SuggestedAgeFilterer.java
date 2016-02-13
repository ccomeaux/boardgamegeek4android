package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class SuggestedAgeFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 21;

	private int min;
	private int max;
	private boolean includeUndefined;

	public SuggestedAgeFilterer(Context context) {
		super(context);
	}

	public SuggestedAgeFilterer(@NonNull Context context, int min, int max, boolean includeUndefined) {
		super(context);
		this.min = min;
		this.max = max;
		this.includeUndefined = includeUndefined;
		init(context);
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		includeUndefined = (d[2].equals("1"));
		init(context);
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_SUGGESTED_AGE;
	}

	private void init(@NonNull Context context) {
		setSelection();
	}

	@Override
	public String getDisplayText() {
		String text;
		String minText = String.valueOf(min);
		String maxText = String.valueOf(max);

		if (max == MAX_RANGE) {
			text = minText + "+";
		} else if (min == max) {
			text = maxText;
		} else {
			text = minText + "-" + maxText;
		}
		if (includeUndefined) {
			text += " (+?)";
		}
		return context.getString(R.string.ages) + " " + text;
	}

	private void setSelection() {
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);

		String selection;
		if (max == MAX_RANGE) {
			selection = "(" + Games.MINIMUM_AGE + ">=?)";
			selectionArgs(minValue);
		} else {
			selection = "(" + Games.MINIMUM_AGE + ">=? AND " + Games.MINIMUM_AGE + "<=?)";
			selectionArgs(minValue, maxValue);
		}
		if (includeUndefined) {
			selection += " OR " + Games.MINIMUM_AGE + "=0 OR " + Games.MINIMUM_AGE + " IS NULL";
		}
		selection(selection);
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public boolean includeUndefined() {
		return includeUndefined;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max) + DELIMITER + (includeUndefined ? "1" : "0");
	}
}
