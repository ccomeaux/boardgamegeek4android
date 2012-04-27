package com.boardgamegeek.data;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class PlayTimeFilterData extends CollectionFilterData {
	public static final int MIN_RANGE = 0;
	public static final int MAX_RANGE = 300;

	private int mMin;
	private int mMax;
	private boolean mUndefined;

	public PlayTimeFilterData() {
		id(CollectionFilterDataFactory.ID_PLAY_TIME);
	}

	public PlayTimeFilterData(Context context, int min, int max, boolean undefined) {
		mMin = min;
		mMax = max;
		mUndefined = undefined;

		id(CollectionFilterDataFactory.ID_PLAY_TIME);
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
		displayText(getDisplayText() + " " + r.getString(R.string.time_suffix));
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
}
