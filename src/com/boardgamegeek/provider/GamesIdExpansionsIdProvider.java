package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdExpansionsIdProvider extends BaseProvider {
	GamesIdExpansionsProvider mProvider = new GamesIdExpansionsProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long expansionId = ContentUris.parseId(uri);
		return mProvider.buildSimpleSelection(uri).whereEquals(GamesExpansions.EXPANSION_ID, expansionId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(mProvider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return GamesExpansions.CONTENT_ITEM_TYPE;
	}
}
