package com.boardgamegeek.data;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountFilterData extends CollectionFilterData {
	public static final int MIN_RANGE = 0;
	public static final int MAX_RANGE = 25;

	private static final String delimiter = ":";

	private int mMin;
	private int mMax;

	public PlayCountFilterData() {
		setType(CollectionFilterDataFactory.TYPE_PLAY_COUNT);
	}

	public PlayCountFilterData(Context context, int min, int max) {
		mMin = min;
		mMax = max;
		init(context);
	}

	public PlayCountFilterData(Context context, String data) {
		String[] d = data.split(delimiter);
		mMin = Integer.valueOf(d[0]);
		mMax = Integer.valueOf(d[1]);
		init(context);
	}

	@Override
	public String flatten() {
		return String.valueOf(mMin) + delimiter + String.valueOf(mMax);
	}

	public int getMax() {
		return mMax;
	}

	public int getMin() {
		return mMin;
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_PLAY_COUNT);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String text = "";
		if (mMax >= MAX_RANGE) {
			text = mMin + "+";
		} else if (mMin == mMax) {
			text = String.valueOf(mMax);
		} else {
			text = mMin + "-" + mMax;
		}
		displayText(text + " " + r.getString(R.string.plays));
	}

	private void setSelection() {
		String selection = "";
		if (mMax >= MAX_RANGE) {
			selection = Collection.NUM_PLAYS + ">=?";
			selectionArgs(String.valueOf(MAX_RANGE));
		} else {
			selection = "(" + Collection.NUM_PLAYS + ">=? AND " + Collection.NUM_PLAYS + "<=?)";
			selectionArgs(String.valueOf(mMin), String.valueOf(mMax));
		}
		selection(selection);
	}
}
