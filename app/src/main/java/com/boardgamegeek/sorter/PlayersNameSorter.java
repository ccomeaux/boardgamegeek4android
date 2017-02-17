package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlayersNameSorter extends PlayersSorter {
	public PlayersNameSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.menu_sort_name;
	}

	@Override
	public int getType() {
		return PlayersSorterFactory.TYPE_NAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { PlayPlayers.NAME, PlayPlayers.SUM_QUANTITY };
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getFirstChar(cursor, PlayPlayers.NAME);
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		int playCount = getInt(cursor, Plays.SUM_QUANTITY);
		return context.getResources().getQuantityString(R.plurals.plays_suffix, playCount, playCount);
	}
}
