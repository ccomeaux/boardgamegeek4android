package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionFilterDetails;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionFiltersProvider extends BaseProvider {

	private String table = Tables.COLLECTION_FILTERS;

	@Override
	public SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(table);
	}

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		deleteCollectionFilterChildren(db, builder);
	}

	@Override
	protected String getDefaultSortOrder() {
		return CollectionFilters.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "collectionfilters";
	}

	@Override
	public String getType(Uri uri) {
		return CollectionFilters.CONTENT_TYPE;
	}

	@Override
	public Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		long rowId = db.insertOrThrow(table, null, values);
		return CollectionFilters.buildFilterUri(rowId);
	}

	public static void deleteCollectionFilterChildren(SQLiteDatabase db, SelectionBuilder builder) {
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
}
