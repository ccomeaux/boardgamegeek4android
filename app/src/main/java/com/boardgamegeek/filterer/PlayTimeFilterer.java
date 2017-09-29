package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.Locale;

public class PlayTimeFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 0;
	public static final int MAX_RANGE = 300;

	private int min;
	private int max;
	private boolean includeUndefined;

	public PlayTimeFilterer(Context context) {
		super(context);
	}

	public PlayTimeFilterer(@NonNull Context context, int min, int max, boolean includeUndefined) {
		super(context);
		this.min = min;
		this.max = max;
		this.includeUndefined = includeUndefined;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = d.length > 0 ? MathUtils.constrain(StringUtils.parseInt(d[0], MIN_RANGE), MIN_RANGE, MAX_RANGE) : MIN_RANGE;
		max = d.length > 1 ? MathUtils.constrain(StringUtils.parseInt(d[1], MAX_RANGE), MIN_RANGE, MAX_RANGE) : MAX_RANGE;
		includeUndefined = d.length > 2 && (d[2].equals("1"));
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_play_time;
	}

	@Override
	public String getDisplayText() {
		return describe(R.string.unknown_abbr);
	}

	@Override
	public String getDescription() {
		return describe(R.string.unknown);
	}

	@NonNull
	private String describe(@StringRes int unknownResId) {
		String text;
		if (max == MAX_RANGE) {
			text = context.getString(R.string.and_up_suffix_abbr, min);
		} else if (min == max) {
			text = String.format(Locale.getDefault(), "%,d", max);
		} else {
			text = String.format(Locale.getDefault(), "%,d-%,d", min, max);
		}
		text += " " + context.getString(R.string.minutes_abbr);
		if (includeUndefined) text += String.format(" (+%s)", context.getString(unknownResId));
		return text;
	}


	@Override
	public String getSelection() {
		String format = max == MAX_RANGE ? "(%1$s>=?)" : "(%1$s>=? AND %1$s<=?)";
		if (includeUndefined) format += " OR %1$s IS NULL";
		return String.format(format, Games.PLAYING_TIME);
	}

	@Override
	public String[] getSelectionArgs() {
		return max == MAX_RANGE ?
			new String[] { String.valueOf(min) } :
			new String[] { String.valueOf(min), String.valueOf(max) };
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
