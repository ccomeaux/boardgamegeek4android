package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionFilterDetails;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionFiltersIdDetailsIdProvider extends BaseProvider {

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
		return "collectionfilters/#/deatils/#";
	}

	@Override
	protected String getType(Uri uri) {
		return CollectionFilterDetails.CONTENT_ITEM_TYPE;
	}

	private SelectionBuilder buildSelection(Uri uri, String table) {
		long filterId = CollectionFilters.getFilterId(uri);
		int type = CollectionFilterDetails.getFilterType(uri);
		return new SelectionBuilder().table(table)
				.where(CollectionFilterDetails.FILTER_ID + "=?", String.valueOf(filterId))
				.where(CollectionFilterDetails.TYPE + "=?", String.valueOf(type));
	}
}
