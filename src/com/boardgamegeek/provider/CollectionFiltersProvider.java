package com.boardgamegeek.provider;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionFilterDetails;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionFiltersProvider extends BasicProvider {

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		// TODO after upgrading to API 8, use cascading deletes (http://stackoverflow.com/questions/2545558)
		Cursor c = builder.query(db, new String[] { CollectionFilters._ID }, null);
		try {
			while (c.moveToNext()) {
				int filterId = c.getInt(0);
				String[] filterArg = new String[] { String.valueOf(filterId) };
				db.delete(Tables.COLLECTION_FILTERS_DETAILS, CollectionFilterDetails.FILTER_ID + "=?", filterArg);
			}
		} finally {
			c.close();
		}
	}

	@Override
	protected String getDefaultSortOrder() {
		return CollectionFilters.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_COLLECTION_FILTERS;
	}

	@Override
	protected String getTable() {
		return Tables.COLLECTION_FILTERS;
	}

	@Override
	public String getType(Uri uri) {
		return CollectionFilters.CONTENT_TYPE;
	}
}
