package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionFilterDetails;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionFiltersIdDetailsProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return buildSelection(uri, Tables.COLLECTION_FILTERS_DETAILS_JOIN_COLLECTION_FILTERS);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return buildSelection(uri, Tables.COLLECTION_FILTERS_DETAILS);
	}

	@Override
	protected String getPath() {
		return "collectionfilters/#/details";
	}

	@Override
	protected String getType(Uri uri) {
		return CollectionFilterDetails.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		long filterId = CollectionFilters.getFilterId(uri);
		values.put(CollectionFilterDetails.FILTER_ID, filterId);
		long rowId = db.insertOrThrow(Tables.COLLECTION_FILTERS_DETAILS, null, values);
		Uri newUri = CollectionFilters.buildFilterDetailUri(filterId, rowId);
		return newUri;
	}

	private SelectionBuilder buildSelection(Uri uri, String table) {
		long filterId = CollectionFilters.getFilterId(uri);
		return new SelectionBuilder().table(table).where(CollectionFilterDetails.FILTER_ID + "=?",
				String.valueOf(filterId));
	}
}
