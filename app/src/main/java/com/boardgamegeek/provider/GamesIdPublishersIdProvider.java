package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggDatabase.GamesPublishers;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPublishersIdProvider extends BaseProvider {
	final GamesIdPublishersProvider provider = new GamesIdPublishersProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		final long publisherId = ContentUris.parseId(uri);
		return provider.buildSimpleSelection(uri).whereEquals(GamesPublishers.PUBLISHER_ID, publisherId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(provider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Publishers.CONTENT_ITEM_TYPE;
	}
}
