package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class PlayTimeFilterData extends CollectionFilterData {
	public static final int MIN_RANGE = 0;
	public static final int MAX_RANGE = 300;

	private int mMin;
	private int mMax;
	private boolean mUndefined;

	public PlayTimeFilterData(Context context, int min, int max, boolean undefined) {
		mMin = min;
		mMax = max;
		mUndefined = undefined;

		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		id(R.id.menu_play_time);
		String namePrefix = "";				
		if(max == MAX_RANGE) {
			selection("(" + Games.PLAYING_TIME + ">=?)");
			selectionArgs(minValue);
		} else {
			selection("(" + Games.PLAYING_TIME + ">=? AND " + Games.PLAYING_TIME + "<=?)");
			selectionArgs(minValue, maxValue);
		}
		
		if(mUndefined) {
			selection(getSelection() + " OR " + Games.PLAYING_TIME + " IS NULL");
		}
					
		if (mMin == mMax) {
			namePrefix = maxValue;
		} else {
			namePrefix = minValue + "-" + maxValue;
		}
		name(namePrefix + " " + context.getResources().getString(R.string.playing_time));
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
