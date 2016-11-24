package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdExpansionsIdProvider extends BaseProvider {
	final GamesIdExpansionsProvider provider = new GamesIdExpansionsProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long expansionId = ContentUris.parseId(uri);
		return provider.buildSimpleSelection(uri).whereEquals(GamesExpansions.EXPANSION_ID, expansionId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(provider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return GamesExpansions.CONTENT_ITEM_TYPE;
	}
}
