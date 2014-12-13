package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionViewIdFiltersIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS_JOIN_COLLECTION_VIEWS);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS);
	}

	@Override
	protected String getPath() {
		return "collectionviews/#/filters/#";
	}

	@Override
	protected String getType(Uri uri) {
		return CollectionViewFilters.CONTENT_ITEM_TYPE;
	}

	private SelectionBuilder buildSelection(Uri uri, String table) {
		long filterId = CollectionViews.getViewId(uri);
		int type = CollectionViewFilters.getFilterType(uri);
		return new SelectionBuilder().table(table)
				.where(CollectionViewFilters.VIEW_ID + "=?", String.valueOf(filterId))
				.where(CollectionViewFilters.TYPE + "=?", String.valueOf(type));
	}
}
