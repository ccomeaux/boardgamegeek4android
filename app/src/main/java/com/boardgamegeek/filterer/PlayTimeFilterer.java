package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

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
		init(context);
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		includeUndefined = (d[2].equals("1"));
		init(context);
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_PLAY_TIME;
	}

	private void init(@NonNull Context context) {
		setSelection();
	}

	@Override
	public String getDisplayText() {
		String text;
		String minText = String.valueOf(min);
		String maxText = String.valueOf(max);

		if (max == MAX_RANGE) {
			text = minText + "+";
		} else if (min == max) {
			text = maxText;
		} else {
			text = minText + "-" + maxText;
		}
		if (includeUndefined) {
			text += " (+?)";
		}
		return text + " " + context.getResources().getString(R.string.minutes_abbr);
	}

	private void setSelection() {
		if (max == MAX_RANGE) {
			selection("(" + Games.PLAYING_TIME + ">=?)");
		} else {
			selection("(" + Games.PLAYING_TIME + ">=? AND " + Games.PLAYING_TIME + "<=?)");
		}
		if (includeUndefined) {
			selection(getSelection() + " OR " + Games.PLAYING_TIME + " IS NULL");
		}
	}

	@Override
	public String[] getSelectionArgs() {
		if (max == MAX_RANGE) {
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

	public boolean includeUndefined() {
		return includeUndefined;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max) + DELIMITER + (includeUndefined ? "1" : "0");
	}
}
