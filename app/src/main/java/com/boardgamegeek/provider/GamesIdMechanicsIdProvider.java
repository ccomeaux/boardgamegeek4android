package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggDatabase.GamesMechanics;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdMechanicsIdProvider extends BaseProvider {
	GamesIdMechanicsProvider mProvider = new GamesIdMechanicsProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		final long mechanicId = ContentUris.parseId(uri);
		return mProvider.buildSimpleSelection(uri).whereEquals(GamesMechanics.MECHANIC_ID, mechanicId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(mProvider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Mechanics.CONTENT_ITEM_TYPE;
	}
}
