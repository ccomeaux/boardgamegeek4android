package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 0;
	public static final int MAX_RANGE = 25;

	private int min;
	private int max;

	public PlayCountFilterer() {
		setType(CollectionFilterDataFactory.TYPE_PLAY_COUNT);
	}

	public PlayCountFilterer(Context context, int min, int max) {
		this.min = min;
		this.max = max;
		init(context);
	}

	public PlayCountFilterer(Context context, String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		init(context);
	}

	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max);
	}

	public int getMax() {
		return max;
	}

	public int getMin() {
		return min;
	}

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_PLAY_COUNT);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
		String text;
		if (max >= MAX_RANGE) {
			text = min + "+";
		} else if (min == max) {
			text = String.valueOf(max);
		} else {
			text = min + "-" + max;
		}
		displayText(text + " " + r.getString(R.string.plays));
	}

	private void setSelection() {
		String selection;
		if (max >= MAX_RANGE) {
			selection = Collection.NUM_PLAYS + ">=?";
			selectionArgs(String.valueOf(min));
		} else {
			selection = "(" + Collection.NUM_PLAYS + ">=? AND " + Collection.NUM_PLAYS + "<=?)";
			selectionArgs(String.valueOf(min), String.valueOf(max));
		}
		selection(selection);
	}
}
