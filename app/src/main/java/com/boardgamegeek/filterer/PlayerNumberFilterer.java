package com.boardgamegeek.filterer;

import android.content.Context;
import android.content.res.Resources;
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
		setType(CollectionFilterDataFactory.TYPE_PLAYER_NUMBER);
	}

	public PlayerNumberFilterer(@NonNull Context context, @NonNull String data) {
		super(context);
		setData(data);
		init(context);
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
	}

	private void init(@NonNull Context context) {
		setType(CollectionFilterDataFactory.TYPE_PLAYER_NUMBER);
		setDisplayText(context.getResources());
		setSelection();
	}

	private void setDisplayText(@NonNull Resources r) {
		String range = "";
		if (isExact) {
			range = r.getString(R.string.exactly) + " ";
		}
		if (min == max) {
			range += String.valueOf(max);
		} else {
			range += String.valueOf(min) + "-" + String.valueOf(max);
		}
		displayText(range + " " + r.getString(R.string.players));
	}

	private void setSelection() {
		String minValue = String.valueOf(min);
		String maxValue = String.valueOf(max);

		if (isExact) {
			selection(Games.MIN_PLAYERS + "=? AND " + Games.MAX_PLAYERS + "=?");
			selectionArgs(minValue, maxValue);
		} else {
			selection(Games.MIN_PLAYERS + "<=? AND (" + Games.MAX_PLAYERS + ">=?" + " OR " + Games.MAX_PLAYERS
				+ " IS NULL)");
			selectionArgs(minValue, maxValue);
		}
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
