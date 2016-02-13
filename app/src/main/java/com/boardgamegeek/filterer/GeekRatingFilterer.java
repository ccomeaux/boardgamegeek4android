package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.MathUtils;

public class GeekRatingFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 1.0;
	public static final double MAX_RANGE = 10.0;

	private Context context;
	private double min;
	private double max;
	private boolean includeUnrated;

	public GeekRatingFilterer(Context context) {
		super(context);
		setType(CollectionFiltererFactory.TYPE_GEEK_RATING);
	}

	public GeekRatingFilterer(@NonNull Context context, double min, double max, boolean includeUnrated) {
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
		init(context);
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max) + DELIMITER + (includeUnrated ? "1" : "0");
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	public boolean includeUnrated() {
		return includeUnrated;
	}

	private void init(@NonNull Context context) {
		this.context = context;
		setType(CollectionFiltererFactory.TYPE_GEEK_RATING);
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
		if (includeUnrated) {
			selection += " OR " + Games.STATS_BAYES_AVERAGE + "=0 OR " + Games.STATS_BAYES_AVERAGE + " IS NULL";
		}
		selection(selection);
	}
}
