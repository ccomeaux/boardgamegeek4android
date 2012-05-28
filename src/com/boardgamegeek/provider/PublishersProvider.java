package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class PublishersProvider extends BasicProvider {

	@Override
	protected String getDefaultSortOrder() {
		return Publishers.DEFAULT_SORT;
	}
	
	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Publishers.PUBLISHER_ID);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_PUBLISHERS;
	}

	@Override
	protected String getTable() {
		return Tables.PUBLISHERS;
	}
	
	@Override
	protected String getType(Uri uri) {
		return Publishers.CONTENT_TYPE;
	}
}
