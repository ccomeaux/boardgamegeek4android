package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class AverageRatingFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 0.0;
	public static final double MAX_RANGE = 10.0;

	private double min;
	private double max;

	public AverageRatingFilterer() {
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_RATING);
	}

	public AverageRatingFilterer(@NonNull Context context, @NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Double.valueOf(d[0]);
		max = Double.valueOf(d[1]);
		init(context);
	}

	public AverageRatingFilterer(@NonNull Context context, double min, double max) {
		this.min = min;
		this.max = max;
		init(context);
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
		selection(selection);
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max);
	}
}
