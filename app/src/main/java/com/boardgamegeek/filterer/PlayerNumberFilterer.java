package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.StringUtils;

public class PlayerNumberFilterer extends CollectionFilterer {
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 12;

	private int min;
	private int max;
	private boolean isExact;

	public PlayerNumberFilterer(Context context) {
		super(context);
	}

	public PlayerNumberFilterer(@NonNull Context context, int min, int max, boolean isExact) {
		super(context);
		this.min = min;
		this.max = max;
		this.isExact = isExact;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = d.length > 0 ? MathUtils.constrain(StringUtils.parseInt(d[0], MIN_RANGE), MIN_RANGE, MAX_RANGE) : MIN_RANGE;
		max = d.length > 1 ? MathUtils.constrain(StringUtils.parseInt(d[1], MAX_RANGE), MIN_RANGE, MAX_RANGE) : MAX_RANGE;
		isExact = d.length > 2 && (d[2].equals("1"));
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_number_of_players;
	}

	@Override
	public String getDisplayText() {
		String range = "";
		if (isExact) {
			range = context.getString(R.string.exactly) + " ";
		}
		if (min == max) {
			range += String.valueOf(max);
		} else {
			range += String.valueOf(min) + "-" + String.valueOf(max);
		}
		return range + " " + context.getString(R.string.players);
	}

	@Override
	public String getSelection() {
		if (isExact) {
			return Games.MIN_PLAYERS + "=? AND " + Games.MAX_PLAYERS + "=?";
		} else {
			return Games.MIN_PLAYERS + "<=? AND (" + Games.MAX_PLAYERS + ">=?" + " OR " + Games.MAX_PLAYERS + " IS NULL)";
		}
	}

	@Override
	public String[] getSelectionArgs() {
		return new String[] { String.valueOf(min), String.valueOf(max) };
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public boolean isExact() {
		return isExact;
	}

	@NonNull
	@Override
	public String flatten() {
		return String.valueOf(min) + DELIMITER + String.valueOf(max) + DELIMITER + (isExact ? "1" : "0");
	}
}
