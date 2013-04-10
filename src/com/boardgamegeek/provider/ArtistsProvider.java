package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class ArtistsProvider extends BasicProvider {
	
	@Override
	protected String getDefaultSortOrder() {
		return Artists.DEFAULT_SORT;
	}

	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Artists.ARTIST_ID);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_ARTISTS;
	}

	@Override
	protected String getTable() {
		return Tables.ARTISTS;
	}

	@Override
	protected String getType(Uri uri) {
		return Artists.CONTENT_TYPE;
	}
}
