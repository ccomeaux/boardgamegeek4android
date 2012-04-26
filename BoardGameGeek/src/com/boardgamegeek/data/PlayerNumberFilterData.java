package com.boardgamegeek.data;

import android.content.Context;
import android.content.res.Resources;

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

		id(R.id.menu_number_of_players);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String range = "";
		if (mExact) {
			range = r.getString(R.string.exactly) + " ";
		}
		if (mExact || mMin == mMax) {
			range += String.valueOf(mMax);
		} else {
			range += String.valueOf(mMin) + "-" + String.valueOf(mMax);
		}
		displayText(range + " " + r.getString(R.string.players));
	}

	private void setSelection() {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		if (mExact) {
			selection(Games.MIN_PLAYERS + "=? AND " + Games.MAX_PLAYERS + "=?");
			selectionArgs(maxValue, maxValue);
		} else {
			selection(Games.MIN_PLAYERS + "<=? AND (" + Games.MAX_PLAYERS + ">=?" + " OR " + Games.MAX_PLAYERS
					+ " IS NULL)");
			selectionArgs(minValue, maxValue);
		}
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
