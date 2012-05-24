package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PublishersIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int publisherId = Publishers.getPublisherId(uri);
		return new SelectionBuilder().table(Tables.PUBLISHERS).whereEquals(Publishers.PUBLISHER_ID, publisherId);
	}

	@Override
	protected String getPath() {
		return "publishers/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Publishers.CONTENT_ITEM_TYPE;
	}
}
