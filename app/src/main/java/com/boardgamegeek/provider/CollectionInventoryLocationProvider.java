package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionInventoryLocationProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder()
			.table(Tables.COLLECTION)
			.groupBy(Collection.PRIVATE_INFO_INVENTORY_LOCATION);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Collection.SORT_INVENTORY_LOCATION;
	}

	@Override
	protected String getPath() {
		return String.format("%s/%s", BggContract.PATH_COLLECTION, BggContract.PATH_INVENTORY_LOCATION);
	}
}
