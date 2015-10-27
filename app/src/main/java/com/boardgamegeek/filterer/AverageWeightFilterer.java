package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class AverageWeightFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 1.0;
	public static final double MAX_RANGE = 5.0;

	private double min;
	private double max;
	private boolean includeUndefined;

	public AverageWeightFilterer() {
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT);
	}

	public AverageWeightFilterer(@NonNull Context context, @NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Double.valueOf(d[0]);
		max = Double.valueOf(d[1]);
		includeUndefined = (d[2].equals("1"));
		init(context);
	}

	public AverageWeightFilterer(@NonNull Context context, double min, double max, boolean includeUndefined) {
		this.min = min;
		this.max = max;
		this.includeUndefined = includeUndefined;
		init(context);
	}

	private void init(@NonNull Context context) {
		setType(CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT);
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
		if (includeUndefined) {
			text += " (+?)";
		}

		displayText(r.getString(R.string.weight) + " " + text);
	}

	private void setSelection() {
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);

		String selection;
		selection = "(" + Games.STATS_AVERAGE_WEIGHT + ">=? AND " + Games.STATS_AVERAGE_WEIGHT + "<=?)";
		selectionArgs(minValue, maxValue);
		if (includeUndefined) {
			selection += " OR " + Games.STATS_AVERAGE_WEIGHT + "=0 OR " + Games.STATS_AVERAGE_WEIGHT + " IS NULL";
		}
		selection(selection);
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
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
