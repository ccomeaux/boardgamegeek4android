package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class CollectionViewProvider extends BasicProvider {

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
