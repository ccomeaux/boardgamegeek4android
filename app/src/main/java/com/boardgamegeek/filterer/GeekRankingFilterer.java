package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Games;

public class GeekRankingFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 2000;

	private int min;
	private int max;
	private boolean includeUnranked;

	public GeekRankingFilterer(Context context) {
		super(context);
	}

	public GeekRankingFilterer(Context context, int min, int max, boolean includeUnranked) {
		super(context);
		this.min = min;
		this.max = max;
		this.includeUnranked = includeUnranked;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		includeUnranked = Boolean.valueOf(d[2]);
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_GEEK_RANKING;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max) + DELIMITER + (includeUnranked ? "1" : "0");
	}

	public int getMax() {
		return max;
	}

	public int getMin() {
		return min;
	}

	public boolean includeUnranked() {
		return includeUnranked;
	}

	@Override
	public String getDisplayText() {
		String minText = String.valueOf(min);
		String maxText = String.valueOf(max);

		String text;
		if (min >= MAX_RANGE) {
			text = MAX_RANGE + "+";
		} else if (min == max) {
			text = maxText;
		} else {
			text = minText + "-" + maxText;
		}
		if (includeUnranked) {
			text += " (+?)";
		}
		return "#" + text;
	}

	@Override
	public String getSelection() {
		String selection;
		if (min >= MAX_RANGE) {
			selection = Games.GAME_RANK + ">=?";
		} else {
			selection = "(" + Games.GAME_RANK + ">=? AND " + Games.GAME_RANK + "<=?)";
		}
		if (includeUnranked) {
			selection += " OR " + Games.GAME_RANK + "=0 OR " + Games.GAME_RANK + " IS NULL";
		}
		return selection;
	}

	@Override
	public String[] getSelectionArgs() {
		if (min >= MAX_RANGE) {
			return new String[] { String.valueOf(MAX_RANGE) };
		} else {
			return new String[] { String.valueOf(min), String.valueOf(max) };
		}
	}
}
