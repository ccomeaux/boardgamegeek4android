package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.StringUtils;

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
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = d.length > 0 ? MathUtils.constrain(StringUtils.parseInt(d[0], MIN_RANGE), MIN_RANGE, MAX_RANGE) : MIN_RANGE;
		max = d.length > 1 ? MathUtils.constrain(StringUtils.parseInt(d[1], MAX_RANGE), MIN_RANGE, MAX_RANGE) : MAX_RANGE;
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_play_count;
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

	@Override
	public String getSelection() {
		String selection;
		if (max >= MAX_RANGE) {
			selection = Collection.NUM_PLAYS + ">=?";
		} else {
			selection = "(" + Collection.NUM_PLAYS + ">=? AND " + Collection.NUM_PLAYS + "<=?)";
		}
		return selection;
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
