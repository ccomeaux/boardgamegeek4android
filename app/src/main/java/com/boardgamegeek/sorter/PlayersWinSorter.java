package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlayersWinSorter extends PlayersSorter {
	public PlayersWinSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Plays.SUM_WINS, true);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.menu_sort_wins;
	}

	@Override
	public int getType() {
		return PlayersSorterFactory.TYPE_WINS;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.SUM_WINS };
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		int q = getInt(cursor, Plays.SUM_WINS);
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
		int winCount = getInt(cursor, Plays.SUM_WINS);
		return context.getResources().getQuantityString(R.plurals.wins_suffix, winCount, winCount);
	}
}
