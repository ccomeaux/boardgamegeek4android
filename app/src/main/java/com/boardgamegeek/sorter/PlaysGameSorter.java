package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayItems;

public class PlaysGameSorter extends PlaysSorter {
	public PlaysGameSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause("play_items." + PlayItems.NAME, false);
		descriptionId = R.string.menu_plays_sort_game;
	}

	@Override
	public int getType() {
		return PlaysSorterFactory.TYPE_PLAY_GAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { PlayItems.NAME, PlayItems.OBJECT_ID };
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getString(cursor, PlayItems.NAME);
	}

	@Override
	public long getHeaderId(@NonNull Cursor cursor) {
		return getLong(cursor, PlayItems.OBJECT_ID);
	}
}
