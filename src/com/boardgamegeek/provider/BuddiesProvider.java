package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class BuddiesProvider extends BasicProvider {

	@Override
	protected String getTable() {
		return Tables.BUDDIES;
	}

	@Override
	protected String getDefaultSortOrder() {
		return Buddies.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_BUDDIES;
	}

	@Override
	protected String getType(Uri uri) {
		return Buddies.CONTENT_TYPE;
	}

	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Buddies.BUDDY_ID);
	}
}
