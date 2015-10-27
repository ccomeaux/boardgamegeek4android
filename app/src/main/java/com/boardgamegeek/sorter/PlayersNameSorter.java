package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayPlayers;

public class PlayersNameSorter extends PlayersSorter {
	public PlayersNameSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(PlayPlayers.NAME, false);
		descriptionId = R.string.menu_sort_name;
	}

	@Override
	public int getType() {
		return PlayersSorterFactory.TYPE_NAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { PlayPlayers.NAME };
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getFirstChar(cursor, PlayPlayers.NAME);
	}
}
