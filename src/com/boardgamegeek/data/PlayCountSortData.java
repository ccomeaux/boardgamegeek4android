package com.boardgamegeek.data;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Collection;

public abstract class PlayCountSortData extends CollectionSortData {
	public PlayCountSortData(Context context) {
		super(context);
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.NUM_PLAYS };
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		// TODO: need to use R
		return getScrollText(cursor) + " Plays";
	}

	@Override
	public String getScrollText(Cursor cursor) {
		return getIntAsString(cursor, Collection.NUM_PLAYS, "0");
	}
}
