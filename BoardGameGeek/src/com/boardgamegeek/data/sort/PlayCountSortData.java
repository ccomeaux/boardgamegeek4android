package com.boardgamegeek.data.sort;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
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
	public String getHeaderText(Cursor cursor) {
		return getIntAsString(cursor, Collection.NUM_PLAYS, "0");
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getHeaderText(cursor) + " " + mContext.getString(R.string.plays);
	}
}
