package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class PlayerNumberFilter extends CollectionFilter {
	public static final int MIN_RANGE = 0;
	public static final int MAX_RANGE = 12;

	private int mMin;
	private int mMax;
	private boolean mExact;

	public PlayerNumberFilter(Context context, int min, int max, boolean exact) {
		mMin = min;
		mMax = max;
		mExact = exact;

		String startValue = String.valueOf(mMin);
		String endValue = String.valueOf(mMax);

		// TODO: handle null DB values
		// TODO: treat 12 as 12+
		id(R.id.menu_number_of_players);
		String namePrefix = "";
		if (!mExact) {
			selection(Games.MIN_PLAYERS + "<=? AND " + Games.MAX_PLAYERS + ">=?");
			selectionArgs(endValue, startValue);
			if (mMin == mMax) {
				namePrefix = endValue;
			} else {
				namePrefix = startValue + "-" + endValue;
			}
		} else {
			selection(Games.MIN_PLAYERS + "=? AND " + Games.MAX_PLAYERS + "=?");
			selectionArgs(endValue, endValue);
			namePrefix = endValue;
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
