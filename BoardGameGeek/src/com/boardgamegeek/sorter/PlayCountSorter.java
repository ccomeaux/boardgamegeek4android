package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public abstract class PlayCountSorter extends CollectionSorter {
	public PlayCountSorter(Context context) {
		super(context);
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.NUM_PLAYS };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		return getIntAsString(cursor, Collection.NUM_PLAYS, "0");
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getHeaderText(cursor) + " " + mContext.getString(R.string.plays);
	}
}
