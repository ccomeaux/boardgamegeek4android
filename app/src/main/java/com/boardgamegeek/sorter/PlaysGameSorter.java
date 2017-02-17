package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;

public class PlaysGameSorter extends PlaysSorter {
	public PlaysGameSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.menu_plays_sort_game;
	}

	@Override
	public int getType() {
		return PlaysSorterFactory.TYPE_PLAY_GAME;
	}

	@Override
	protected String getSortColumn() {
		return Plays.ITEM_NAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.ITEM_NAME, Plays.OBJECT_ID };
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getString(cursor, Plays.ITEM_NAME);
	}

	@Override
	public long getHeaderId(@NonNull Cursor cursor) {
		return getLong(cursor, Plays.OBJECT_ID);
	}
}
