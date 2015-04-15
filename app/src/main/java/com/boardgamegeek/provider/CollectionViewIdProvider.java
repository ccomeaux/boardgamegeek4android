package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionViewIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long filterId = CollectionViews.getViewId(uri);
		return new SelectionBuilder().table(Tables.COLLECTION_VIEWS).where(CollectionViews._ID + "=?",
				String.valueOf(filterId));
	}

	@Override
	protected String getPath() {
		return "collectionviews/#";
	}

	@Override
	protected String getType(Uri uri) {
		return CollectionViews.CONTENT_ITEM_TYPE;
	}
}
