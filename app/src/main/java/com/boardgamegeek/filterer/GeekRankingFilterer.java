package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.provider.BggContract.Games;

public class GeekRankingFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 2000;
	private static final String DELIMITER = ":";

	private int min;
	private int max;
	private boolean includeUnranked;

	public GeekRankingFilterer() {
		setType(CollectionFilterDataFactory.TYPE_GEEK_RANKING);
	}

	public GeekRankingFilterer(Context context, int min, int max, boolean includeUnranked) {
		this.min = min;
		this.max = max;
		this.includeUnranked = includeUnranked;
		init(context);
	}

	public GeekRankingFilterer(Context context, String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		includeUnranked = Boolean.valueOf(d[2]);
		init(context);
	}

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

	private void init(Context context) {
		setType(CollectionFilterDataFactory.TYPE_GEEK_RANKING);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(Resources r) {
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
		displayText("#" + text);
	}

	private void setSelection() {
		String selection;
		if (min >= MAX_RANGE) {
			selection = Games.GAME_RANK + ">=?";
			selectionArgs(String.valueOf(MAX_RANGE));
		} else {
			selection = "(" + Games.GAME_RANK + ">=? AND " + Games.GAME_RANK + "<=?)";
			selectionArgs(String.valueOf(min), String.valueOf(max));
		}
		if (includeUnranked) {
			selection += " OR " + Games.GAME_RANK + "=0 OR " + Games.GAME_RANK + " IS NULL";
		}
		selection(selection);
	}
}
