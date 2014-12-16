package com.boardgamegeek.data;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class PlayerNumberFilterData extends CollectionFilterData {
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 12;

	private static final String delimiter = ":";

	private int mMin;
	private int mMax;
	private boolean mExact;

	public PlayerNumberFilterData() {
		setType(CollectionFilterDataFactory.TYPE_PLAYER_NUMBER);
	}

	public PlayerNumberFilterData(Context context, String data) {
		String[] d = data.split(delimiter);
		mMin = Integer.valueOf(d[0]);
		mMax = Integer.valueOf(d[1]);
		mExact = (d[2].equals("1"));
		init(context);
	}

	public PlayerNumberFilterData(Context context, int min, int max, boolean exact) {
		mMin = min;
		mMax = max;
		mExact = exact;
		init(context);
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_PLAYER_NUMBER);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String range = "";
		if (mExact) {
			range = r.getString(R.string.exactly) + " ";
		}
		if (mMin == mMax) {
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
			selectionArgs(minValue, maxValue);
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

	@Override
	public String flatten() {
		return String.valueOf(mMin) + delimiter + String.valueOf(mMax) + delimiter + (mExact ? "1" : "0");
	}
}
