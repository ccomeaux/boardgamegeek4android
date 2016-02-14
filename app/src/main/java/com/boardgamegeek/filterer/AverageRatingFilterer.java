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
	}

	public AverageRatingFilterer(@NonNull Context context, double min, double max, boolean includeUnrated) {
		super(context);
		this.min = min;
		this.max = max;
		this.includeUnrated = includeUnrated;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = MathUtils.constrain(Double.valueOf(d[0]), MIN_RANGE, MAX_RANGE);
		max = MathUtils.constrain(Double.valueOf(d[1]), MIN_RANGE, MAX_RANGE);
		includeUnrated = d.length > 2 ? (d[2].equals("1")) : (min < 1.0);
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_AVERAGE_RATING;
	}

	@Override
	public String getDisplayText() {
		Resources r = context.getResources();
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
		return r.getString(R.string.average) + " " + text;
	}

	@Override
	public String getSelection() {
		String selection;
		if (min == max) {
			selection = Games.STATS_AVERAGE + "=?";
		} else {
			selection = "(" + Games.STATS_AVERAGE + ">=? AND " + Games.STATS_AVERAGE + "<=?)";
		}
		if (includeUnrated) {
			selection += " OR " + Games.STATS_AVERAGE + "=0 OR " + Games.STATS_AVERAGE + " IS NULL";
		}
		return selection;
	}

	@Override
	public String[] getSelectionArgs() {
		if (min == max) {
			return new String[] { String.valueOf(min) };
		} else {
			return new String[] { String.valueOf(min), String.valueOf(max) };
		}
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
