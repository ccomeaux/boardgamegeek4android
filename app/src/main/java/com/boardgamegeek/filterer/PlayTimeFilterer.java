package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
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
		setType(CollectionFilterDataFactory.TYPE_PLAY_TIME);
	}

	public PlayTimeFilterer(@NonNull Context context, @NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		includeUndefined = (d[2].equals("1"));
		init(context);
	}

	public PlayTimeFilterer(@NonNull Context context, int min, int max, boolean includeUndefined) {
		this.min = min;
		this.max = max;
		this.includeUndefined = includeUndefined;
		init(context);
	}

	private void init(@NonNull Context context) {
		setType(CollectionFilterDataFactory.TYPE_PLAY_TIME);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(@NonNull Resources r) {
		String minText = String.valueOf(min);
		String maxText = String.valueOf(max);

		if (max == MAX_RANGE) {
			displayText(minText + "+");
		} else if (min == max) {
			displayText(maxText);
		} else {
			displayText(minText + "-" + maxText);
		}
		if (includeUndefined) {
			displayText(getDisplayText() + " (+?)");
		}
		displayText(getDisplayText() + " " + r.getString(R.string.minutes_abbr));
	}

	private void setSelection() {
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);

		if (max == MAX_RANGE) {
			selection("(" + Games.PLAYING_TIME + ">=?)");
			selectionArgs(minValue);
		} else {
			selection("(" + Games.PLAYING_TIME + ">=? AND " + Games.PLAYING_TIME + "<=?)");
			selectionArgs(minValue, maxValue);
		}

		if (includeUndefined) {
			selection(getSelection() + " OR " + Games.PLAYING_TIME + " IS NULL");
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
