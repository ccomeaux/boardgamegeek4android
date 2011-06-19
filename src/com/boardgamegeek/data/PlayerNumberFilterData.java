package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class PlayerNumberFilterData extends CollectionFilterData {
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 12;

	private int mMin;
	private int mMax;
	private boolean mExact;

	public PlayerNumberFilterData(Context context, int min, int max, boolean exact) {
		mMin = min;
		mMax = max;
		mExact = exact;

		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		id(R.id.menu_number_of_players);
		String namePrefix = "";
		if (!mExact) {						
			selection(Games.MIN_PLAYERS + "<=? AND (" + Games.MAX_PLAYERS + ">=?" +
					" OR " + Games.MAX_PLAYERS + " IS NULL)");			
			selectionArgs(minValue, maxValue);
			
			if (mMin == mMax) {
				namePrefix = maxValue;
			} else {
				namePrefix = minValue + "-" + maxValue;
			}
		} else {
			selection(Games.MIN_PLAYERS + "=? AND " + Games.MAX_PLAYERS + "=?");
			selectionArgs(maxValue, maxValue);
			namePrefix = maxValue;
		}
		name(namePrefix + " " + context.getResources().getString(R.string.players));
	}

	public int getMin() {
		return mMin;
	}

	public int getMax() {
		return mMax;
	}

	public boolean isExact() {
		return mExact;
	}
}
