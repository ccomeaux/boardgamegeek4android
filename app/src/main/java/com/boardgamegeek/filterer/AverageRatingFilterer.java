package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.MathUtils;

public class AverageRatingFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 1.0;
	public static final double MAX_RANGE = 10.0;

	private double min;
	private double max;
	private boolean includeUnrated;

	public AverageRatingFilterer(Context context) {
		super(context);
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_RATING);
	}

	public AverageRatingFilterer(@NonNull Context context, @NonNull String data) {
		super(context);
		setData(data);
		init(context);
	}

	public AverageRatingFilterer(@NonNull Context context, double min, double max, boolean includeUnrated) {
		super(context);
		this.min = min;
		this.max = max;
		this.includeUnrated = includeUnrated;
		init(context);
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = MathUtils.constrain(Double.valueOf(d[0]), MIN_RANGE, MAX_RANGE);
		max = MathUtils.constrain(Double.valueOf(d[1]), MIN_RANGE, MAX_RANGE);
		includeUnrated = d.length <= 2 || (d[2].equals("1"));
	}

	private void init(@NonNull Context context) {
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_RATING);
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
		displayText(r.getString(R.string.average) + " " + text);
	}

	private void setSelection() {
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);

		String selection;
		if (min == max) {
			selection = Games.STATS_AVERAGE + "=?";
			selectionArgs(minValue);
		} else {
			selection = "(" + Games.STATS_AVERAGE + ">=? AND " + Games.STATS_AVERAGE + "<=?)";
			selectionArgs(minValue, maxValue);
		}
		if (includeUnrated) {
			selection += " OR " + Games.STATS_AVERAGE + "=0 OR " + Games.STATS_AVERAGE + " IS NULL";
		}
		selection(selection);
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public boolean includeUnrated() {
		return includeUnrated;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max) + DELIMITER + (includeUnrated ? "1" : "0");
	}
}
