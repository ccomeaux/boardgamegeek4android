package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayPlayers;

public class PlayersNameSorter extends PlayersSorter {

	public PlayersNameSorter(Context context) {
		super(context);
		mOrderByClause = getClause(PlayPlayers.NAME, false);
		mDescriptionId = R.string.menu_sort_name;
	}

	@Override
	public int getType() {
		return PlayersSorterFactory.TYPE_NAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { PlayPlayers.NAME };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getFirstChar(cursor, PlayPlayers.NAME);
	}
}
