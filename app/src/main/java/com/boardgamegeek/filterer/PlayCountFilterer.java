package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 0;
	public static final int MAX_RANGE = 25;

	private int min;
	private int max;

	public PlayCountFilterer(Context context) {
		super(context);
	}

	public PlayCountFilterer(@NonNull Context context, int min, int max) {
		super(context);
		this.min = min;
		this.max = max;
		init(context);
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		init(context);
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_PLAY_COUNT;
	}

	@NonNull
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

	private void init(@NonNull Context context) {
		setSelection();
	}

	@Override
	public String getDisplayText() {
		String text;
		if (max >= MAX_RANGE) {
			text = min + "+";
		} else if (min == max) {
			text = String.valueOf(max);
		} else {
			text = min + "-" + max;
		}
		return text + " " + context.getString(R.string.plays);
	}

	private void setSelection() {
		String selection;
		if (max >= MAX_RANGE) {
			selection = Collection.NUM_PLAYS + ">=?";
		} else {
			selection = "(" + Collection.NUM_PLAYS + ">=? AND " + Collection.NUM_PLAYS + "<=?)";
		}
		selection(selection);
	}

	@Override
	public String[] getSelectionArgs() {
		if (max >= MAX_RANGE) {
			return new String[] { String.valueOf(min) };
		} else {
			return new String[] { String.valueOf(min), String.valueOf(max) };
		}
	}
}
