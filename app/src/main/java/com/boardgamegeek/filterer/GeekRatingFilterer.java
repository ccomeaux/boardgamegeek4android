package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class GeekRatingFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 0.0;
	public static final double MAX_RANGE = 10.0;

	private double min;
	private double max;

	public GeekRatingFilterer() {
		setType(CollectionFilterDataFactory.TYPE_GEEK_RATING);
	}

	public GeekRatingFilterer(@NonNull Context context, double min, double max) {
		this.min = min;
		this.max = max;
		init(context);
	}

	public GeekRatingFilterer(@NonNull Context context, @NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Double.valueOf(d[0]);
		max = Double.valueOf(d[1]);
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

	private void init(@NonNull Context context) {
		setType(CollectionFilterDataFactory.TYPE_GEEK_RATING);
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
		displayText(r.getString(R.string.rating) + " " + text);
	}

	private void setSelection() {
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);

		String selection;
		if (min == max) {
			selection = Games.STATS_BAYES_AVERAGE + "=?";
			selectionArgs(minValue);
		} else {
			selection = "(" + Games.STATS_BAYES_AVERAGE + ">=? AND " + Games.STATS_BAYES_AVERAGE + "<=?)";
			selectionArgs(minValue, maxValue);
		}
		selection(selection);
	}
}
