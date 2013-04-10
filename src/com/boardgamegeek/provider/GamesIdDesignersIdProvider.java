package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdDesignersIdProvider extends BaseProvider {
	GamesIdDesignersProvider mProvider = new GamesIdDesignersProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long designerId = ContentUris.parseId(uri);
		return mProvider.buildSimpleSelection(uri).whereEquals(GamesDesigners.DESIGNER_ID, designerId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(mProvider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Designers.CONTENT_ITEM_TYPE;
	}
}
