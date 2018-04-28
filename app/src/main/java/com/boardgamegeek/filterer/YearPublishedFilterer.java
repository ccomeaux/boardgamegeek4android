package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.Calendar;
import java.util.Locale;

public class YearPublishedFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 1970;
	public static final int MAX_RANGE = Calendar.getInstance().get(Calendar.YEAR) + 1;

	private int min;
	private int max;

	public YearPublishedFilterer(Context context) {
		super(context);
	}

	public YearPublishedFilterer(Context context, int min, int max) {
		super(context);
		this.min = min;
		this.max = max;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = d.length > 0 ? MathUtils.constrain(StringUtils.parseInt(d[0], MIN_RANGE), MIN_RANGE, MAX_RANGE) : MIN_RANGE;
		max = d.length > 1 ? MathUtils.constrain(StringUtils.parseInt(d[1], MAX_RANGE), MIN_RANGE, MAX_RANGE) : MAX_RANGE;
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_year_published;
	}

	@Override
	public String getDisplayText() {
		String text;
		String minText = String.valueOf(min);
		String maxText = String.valueOf(max);

		if (min == MIN_RANGE && max == MAX_RANGE) {
			text = "";
		} else if (min == MIN_RANGE) {
			text = maxText + "-";
		} else if (max == MAX_RANGE) {
			text = minText + "+";
		} else if (min == max) {
			text = maxText;
		} else {
			text = minText + "-" + maxText;
		}

		return text;
	}

	@Override
	public String getDescription() {
		@StringRes int prepositionResId;
		String year;
		if (min == MIN_RANGE && max == MAX_RANGE) {
			return "";
		} else if (min == MIN_RANGE) {
			prepositionResId = R.string.before;
			year = String.valueOf(max + 1);
		} else if (max == MAX_RANGE) {
			prepositionResId = R.string.after;
			year = String.valueOf(min - 1);
		} else if (min == max) {
			prepositionResId = R.string.in;
			year = String.valueOf(max);
		} else {
			prepositionResId = R.string.in;
			year = String.valueOf(min) + "-" + String.valueOf(max);
		}
		return context.getString(R.string.published_prefix, context.getString(prepositionResId), year);
	}

	@Override
	public String getSelection() {
		String format;
		if (min == MIN_RANGE && max == MAX_RANGE) {
			format = "";
		} else if (min == MIN_RANGE) {
			format = "%1$s<=?";
		} else if (max == MAX_RANGE) {
			format = "%1$s>=?";
		} else if (min == max) {
			format = "%1$s=?";
		} else {
			format = "(%1$s>=? AND %1$s<=?)";
		}
		return String.format(Locale.getDefault(), format, Games.YEAR_PUBLISHED);
	}

	@Override
	public String[] getSelectionArgs() {
		if (min == MIN_RANGE && max == MAX_RANGE) {
			return null;
		} else if (min == MIN_RANGE) {
			return new String[] { String.valueOf(max) };
		} else if (max == MAX_RANGE) {
			return new String[] { String.valueOf(min) };
		} else if (min == max) {
			return new String[] { String.valueOf(min) };
		} else {
			return new String[] { String.valueOf(min), String.valueOf(max) };
		}
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max);
	}
}
