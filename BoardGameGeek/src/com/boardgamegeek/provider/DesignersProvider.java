package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class DesignersProvider extends BasicProvider {

	@Override
	protected String getDefaultSortOrder() {
		return Designers.DEFAULT_SORT;
	}

	@Override
	protected String getInsertedIdColumn() {
		return Designers.DESIGNER_ID;
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_DESIGNERS;
	}

	@Override
	protected String getTable() {
		return Tables.DESIGNERS;
	}
	
	@Override
	protected String getType(Uri uri) {
		return Designers.CONTENT_TYPE;
	}
}
