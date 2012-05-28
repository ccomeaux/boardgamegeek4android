package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class MechanicsProvider extends BasicProvider {
	
	@Override
	protected String getDefaultSortOrder() {
		return Mechanics.DEFAULT_SORT;
	}

	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Mechanics.MECHANIC_ID);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_MECHANICS;
	}

	@Override
	public String getTable() {
		return Tables.MECHANICS;
	}

	@Override
	protected String getType(Uri uri) {
		return Mechanics.CONTENT_TYPE;
	}
}
