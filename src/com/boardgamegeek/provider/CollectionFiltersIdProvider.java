package com.boardgamegeek.provider;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionFiltersIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long filterId = CollectionFilters.getFilterId(uri);
		return new SelectionBuilder().table(Tables.COLLECTION_FILTERS).where(CollectionFilters._ID + "=?",
				String.valueOf(filterId));
	}

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		CollectionFiltersProvider.deleteCollectionFilterChildren(db, builder);
	}

	@Override
	protected String getPath() {
		return "collectionfilters/#";
	}

	@Override
	protected String getType(Uri uri) {
		return CollectionFilters.CONTENT_ITEM_TYPE;
	}
}
