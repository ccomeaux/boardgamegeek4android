package com.boardgamegeek.data;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.provider.BggContract.GameRanks;

public class GeekRankingFilterData extends CollectionFilterData {
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 2000;

	private static final String delimiter = ":";

	private int mMin;
	private int mMax;
	private boolean mUnranked;

	public GeekRankingFilterData() {
	}

	public GeekRankingFilterData(Context context, int min, int max, boolean unranked) {
		mMin = min;
		mMax = max;
		mUnranked = unranked;
		init(context);
	}

	public GeekRankingFilterData(Context context, String data) {
		String[] d = data.split(delimiter);
		mMin = Integer.valueOf(d[0]);
		mMax = Integer.valueOf(d[1]);
		mUnranked = Boolean.valueOf(d[2]);
		init(context);
	}

	@Override
	public String flatten() {
		return String.valueOf(mMin) + delimiter + String.valueOf(mMax) + delimiter + (mUnranked ? "1" : "0");
	}

	public int getMax() {
		return mMax;
	}

	public int getMin() {
		return mMin;
	}

	public boolean isUnranked() {
		return mUnranked;
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_GEEK_RANKING);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String minValue = String.valueOf(mMin);
		String maxValue = String.valueOf(mMax);

		String text = "";
		if (mMin >= MAX_RANGE) {
			text = MAX_RANGE + "+";
		} else if (mMin == mMax) {
			text = maxValue;
		} else {
			text = minValue + "-" + maxValue;
		}
		if (mUnranked) {
			text += " (+?)";
		}
		displayText("#" + text);
	}

	private void setSelection() {
		String selection = "";
		if (mMin >= MAX_RANGE) {
			selection = GameRanks.GAME_RANK_VALUE + ">=?";
			selectionArgs(String.valueOf(MAX_RANGE));
		} else {
			selection = "(" + GameRanks.GAME_RANK_VALUE + ">=? AND " + GameRanks.GAME_RANK_VALUE + "<=?)";
			selectionArgs(String.valueOf(mMin), String.valueOf(mMax));
		}
		if (mUnranked) {
			selection += " OR " + GameRanks.GAME_RANK_VALUE + "=0 OR " + GameRanks.GAME_RANK_VALUE + " IS NULL";
		}
		selection(selection);
	}
}
