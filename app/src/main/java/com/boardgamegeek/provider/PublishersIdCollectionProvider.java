package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PublishersIdCollectionProvider extends BaseProvider {
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int publisherId = Publishers.getPublisherId(uri);
		return new SelectionBuilder().table(Tables.PUBLISHER_JOIN_GAMES_JOIN_COLLECTION).whereEquals(Publishers.PUBLISHER_ID, publisherId);
	}

	@Override
	protected String getPath() {
		return "publishers/#/collection";
	}
}
