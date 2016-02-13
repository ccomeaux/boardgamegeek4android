package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

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
		init(context);
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		min = Integer.valueOf(d[0]);
		max = Integer.valueOf(d[1]);
		isExact = (d[2].equals("1"));
		init(context);
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_PLAYER_NUMBER;
	}

	private void init(@NonNull Context context) {
		setSelection();
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

	private void setSelection() {
		if (isExact) {
			selection(Games.MIN_PLAYERS + "=? AND " + Games.MAX_PLAYERS + "=?");
		} else {
			selection(Games.MIN_PLAYERS + "<=? AND (" + Games.MAX_PLAYERS + ">=?" + " OR " + Games.MAX_PLAYERS + " IS NULL)");
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
