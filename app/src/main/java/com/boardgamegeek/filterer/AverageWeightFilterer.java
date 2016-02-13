package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class AverageWeightFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 1.0;
	public static final double MAX_RANGE = 5.0;

	private double min;
	private double max;
	private boolean includeUndefined;

	public AverageWeightFilterer(Context context) {
		super(context);
	}

	public AverageWeightFilterer(@NonNull Context context, double min, double max, boolean includeUndefined) {
		super(context);
		this.min = min;
		this.max = max;
		this.includeUndefined = includeUndefined;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Double.valueOf(d[0]);
		max = Double.valueOf(d[1]);
		includeUndefined = (d[2].equals("1"));
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_AVERAGE_WEIGHT;
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
		if (includeUndefined) {
			text += " (+?)";
		}

		return context.getString(R.string.weight) + " " + text;
	}

	@Override
	public String getSelection() {
		String selection;
		selection = "(" + Games.STATS_AVERAGE_WEIGHT + ">=? AND " + Games.STATS_AVERAGE_WEIGHT + "<=?)";
		if (includeUndefined) {
			selection += " OR " + Games.STATS_AVERAGE_WEIGHT + "=0 OR " + Games.STATS_AVERAGE_WEIGHT + " IS NULL";
		}
		return selection;
	}

	@Override
	public String[] getSelectionArgs() {
		return new String[] { String.valueOf(min), String.valueOf(max) };
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
