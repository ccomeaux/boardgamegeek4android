package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionViewIdFiltersProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS_JOIN_COLLECTION_VIEWS);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS);
	}

	@Override
	protected String getDefaultSortOrder() {
		return CollectionViewFilters.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "collectionviews/#/filters";
	}

	@Override
	protected String getType(Uri uri) {
		return CollectionViewFilters.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		long filterId = CollectionViews.getViewId(uri);
		values.put(CollectionViewFilters.VIEW_ID, filterId);
		long rowId = db.insertOrThrow(Tables.COLLECTION_VIEW_FILTERS, null, values);
		Uri newUri = CollectionViews.buildViewFilterUri(filterId, rowId);
		return newUri;
	}

	private SelectionBuilder buildSelection(Uri uri, String table) {
		long filterId = CollectionViews.getViewId(uri);
		return new SelectionBuilder().table(table)
			.where(CollectionViewFilters.VIEW_ID + "=?", String.valueOf(filterId));
	}
}
