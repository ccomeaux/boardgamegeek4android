package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlayersQuantitySorter extends PlayersSorter {
	public PlayersQuantitySorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.menu_sort_quantity;
	}

	@Override
	public int getType() {
		return PlayersSorterFactory.TYPE_QUANTITY;
	}

	@Override
	protected String getSortColumn() {
		return Plays.SUM_QUANTITY;
	}

	@Override
	protected boolean isSortDescending() {
		return true;
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		int q = getInt(cursor, Plays.SUM_QUANTITY);
		String prefix = String.valueOf(q).substring(0, 1);
		String suffix = "";
		if (q >= 10000) {
			suffix = "0000+";
		} else if (q >= 1000) {
			suffix = "000+";
		} else if (q >= 100) {
			suffix = "00+";
		} else if (q >= 10) {
			suffix = "0+";
		}
		return prefix + suffix;
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		int playCount = getInt(cursor, Plays.SUM_QUANTITY);
		return context.getResources().getQuantityString(R.plurals.plays_suffix, playCount, playCount);
	}
}
