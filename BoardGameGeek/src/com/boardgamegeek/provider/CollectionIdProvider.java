package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int itemId = Collection.getItemId(uri);
		return new SelectionBuilder().table(Tables.COLLECTION).whereEquals(Collection.COLLECTION_ID, itemId);
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int itemId = Collection.getItemId(uri);
		return new SelectionBuilder().table(Tables.COLLECTION_JOIN_GAMES).mapToTable(Collection._ID, Tables.COLLECTION)
			.mapToTable(Collection.GAME_ID, Tables.COLLECTION).mapToTable(Collection.UPDATED, Tables.COLLECTION)
			.mapToTable(Collection.UPDATED_LIST, Tables.COLLECTION)
			.whereEquals(Tables.COLLECTION + "." + Collection.COLLECTION_ID, itemId);
	}

	@Override
	protected String getPath() {
		return "collection/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Collection.CONTENT_ITEM_TYPE;
	}
}
