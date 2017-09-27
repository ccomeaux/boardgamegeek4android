package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.Locale;

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
		min = d.length > 0 ? MathUtils.constrain(StringUtils.parseInt(d[0], MIN_RANGE), MIN_RANGE, MAX_RANGE) : MIN_RANGE;
		max = d.length > 1 ? MathUtils.constrain(StringUtils.parseInt(d[1], MAX_RANGE), MIN_RANGE, MAX_RANGE) : MAX_RANGE;
		includeUnranked = d.length > 2 && (d[2].equals("1"));
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_geek_ranking;
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
		return describeRange(R.string.unranked_abbr);
	}

	@Override
	public String getDescription() {
		return context.getString(R.string.ranked) + " " + describeRange(R.string.unranked);
	}

	private String describeRange(@StringRes int unrankedResId) {
		String text;
		if (min >= MAX_RANGE) {
			text = String.format(Locale.getDefault(), "%,d", MAX_RANGE) + "+";
		} else if (min == max) {
			text = String.format(Locale.getDefault(), "%,d", max);
		} else {
			text = String.format(Locale.getDefault(), "%,d-%,d", min, max);
		}
		if (includeUnranked) text += String.format(" (+%s)", context.getString(unrankedResId));
		return "#" + text;
	}

	@Override
	public String getSelection() {
		String selection = min >= MAX_RANGE ?
			String.format("%s>=?", Games.GAME_RANK) :
			String.format("(%1$s>=? AND %1$s<=?)", Games.GAME_RANK);
		if (includeUnranked) selection += String.format(" OR %1$s=0 OR %1$s IS NULL", Games.GAME_RANK);
		return selection;
	}

	@Override
	public String[] getSelectionArgs() {
		return min >= MAX_RANGE ?
			new String[] { String.valueOf(MAX_RANGE) } :
			new String[] { String.valueOf(min), String.valueOf(max) };
	}
}
