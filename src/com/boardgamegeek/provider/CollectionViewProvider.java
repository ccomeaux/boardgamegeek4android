package com.boardgamegeek.provider;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionViewProvider extends BasicProvider {

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		// TODO after upgrading to API 8, use cascading deletes (http://stackoverflow.com/questions/2545558)
		Cursor c = builder.query(db, new String[] { CollectionViews._ID }, null);
		try {
			while (c.moveToNext()) {
				int filterId = c.getInt(0);
				String[] filterArg = new String[] { String.valueOf(filterId) };
				db.delete(Tables.COLLECTION_VIEW_FILTERS, CollectionViewFilters.VIEW_ID + "=?", filterArg);
			}
		} finally {
			c.close();
		}
	}

	@Override
	protected String getDefaultSortOrder() {
		return CollectionViews.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_COLLECTION_VIEWS;
	}

	@Override
	protected String getTable() {
		return Tables.COLLECTION_VIEWS;
	}

	@Override
	public String getType(Uri uri) {
		return CollectionViews.CONTENT_TYPE;
	}
}
