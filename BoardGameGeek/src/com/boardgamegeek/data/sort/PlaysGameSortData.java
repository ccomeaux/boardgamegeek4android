package com.boardgamegeek.data.sort;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayItems;

public class PlaysGameSortData extends PlaysSortData {

	public PlaysGameSortData(Context context) {
		super(context);
		mOrderByClause = getClause("play_items." + PlayItems.NAME, false);
		mDescriptionId = R.string.menu_plays_sort_game;
	}

	@Override
	public int getType() {
		return PlaysSortDataFactory.TYPE_PLAY_GAME;
	}

	@Override
	public String[] getColumns() {
		return new String[] { PlayItems.NAME, PlayItems.OBJECT_ID };
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getString(cursor, PlayItems.NAME);
	}

	@Override
	public long getHeaderId(Cursor cursor) {
		return getLong(cursor, PlayItems.OBJECT_ID);
	}
}
