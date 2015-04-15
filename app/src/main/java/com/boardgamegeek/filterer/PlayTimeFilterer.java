package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class PlayTimeFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 0;
	public static final int MAX_RANGE = 300;

	private static final String delimiter = ":";

	private int mMin;
	private int mMax;
	private boolean mUndefined;

	public PlayTimeFilterer() {
		setType(CollectionFilterDataFactory.TYPE_PLAY_TIME);
	}

	public PlayTimeFilterer(Context context, String data) {
		String[] d = data.split(delimiter);
		mMin = Integer.valueOf(d[0]);
		mMax = Integer.valueOf(d[1]);
		mUndefined = (d[2].equals("1"));
		init(context);
	}

	public PlayTimeFilterer(Context context, int min, int max, boolean undefined) {
		mMin = min;
		mMax = max;
		mUndefined = undefined;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_PLAY_TIME);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		if (mMax == MAX_RANGE) {
			displayText(minValue + "+");
		} else if (mMin == mMax) {
			displayText(maxValue);
		} else {
			displayText(minValue + "-" + maxValue);
		}
		if (mUndefined) {
			displayText(getDisplayText() + " (+?)");
		}
		displayText(getDisplayText() + " " + r.getString(R.string.minutes_abbr));
	}

	private void setSelection() {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		if (mMax == MAX_RANGE) {
			selection("(" + Games.PLAYING_TIME + ">=?)");
			selectionArgs(minValue);
		} else {
			selection("(" + Games.PLAYING_TIME + ">=? AND " + Games.PLAYING_TIME + "<=?)");
			selectionArgs(minValue, maxValue);
		}

		if (mUndefined) {
			selection(getSelection() + " OR " + Games.PLAYING_TIME + " IS NULL");
		}
	}

	public int getMin() {
		return mMin;
	}

	public int getMax() {
		return mMax;
	}

	public boolean isUndefined() {
		return mUndefined;
	}

	@Override
	public String flatten() {
		return String.valueOf(mMin) + delimiter + String.valueOf(mMax) + delimiter + (mUndefined ? "1" : "0");
	}
}
