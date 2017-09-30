package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.Locale;

public class MyRatingFilterer extends CollectionFilterer {
	public static final double MIN_RANGE = 1.0;
	public static final double MAX_RANGE = 10.0;

	private double min;
	private double max;
	private boolean includeUnrated;

	public MyRatingFilterer(Context context) {
		super(context);
	}

	public MyRatingFilterer(@NonNull Context context, double min, double max, boolean includeUnrated) {
		super(context);
		this.min = min;
		this.max = max;
		this.includeUnrated = includeUnrated;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = d.length > 0 ? MathUtils.constrain(StringUtils.parseDouble(d[0], MIN_RANGE), MIN_RANGE, MAX_RANGE) : MIN_RANGE;
		max = d.length > 1 ? MathUtils.constrain(StringUtils.parseDouble(d[1], MAX_RANGE), MIN_RANGE, MAX_RANGE) : MAX_RANGE;
		includeUnrated = d.length > 2 ? (d[2].equals("1")) : (Double.valueOf(d[0]) < 1.0);
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_my_rating;
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

	@Override
	public String getDisplayText() {
		return context.getResources().getString(R.string.my_rating_abbr) + " " + describeRange(R.string.unrated_abbr);
	}

	@Override
	public String getDescription() {
		return context.getResources().getString(R.string.my_rating) + " " + describeRange(R.string.unrated);
	}

	private String describeRange(@StringRes int unratedResId) {
		String text = min == max ?
			String.format(Locale.getDefault(), "%.1f", max) :
			String.format(Locale.getDefault(), "%.1f-%.1f", min, max);
		if (includeUnrated) text += String.format(" (+%s)", context.getString(unratedResId));
		return text;
	}

	@Override
	public String getSelection() {
		String format = min == max ? "%1$s=?" : "(%1$s>=? AND %1$s<=?)";
		if (includeUnrated) format += " OR %1$s=0 OR %1$s IS NULL";
		return String.format(Locale.getDefault(), format, Collection.RATING);
	}

	@Override
	public String[] getSelectionArgs() {
		return min == max ?
			new String[] { String.valueOf(min) } :
			new String[] { String.valueOf(min), String.valueOf(max) };
	}
}
