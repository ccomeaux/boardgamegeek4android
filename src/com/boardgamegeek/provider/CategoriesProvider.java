package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class CategoriesProvider extends BasicProvider {
	
	@Override
	protected String getDefaultSortOrder() {
		return Categories.DEFAULT_SORT;
	}

	@Override
	protected Integer getInsertedId(ContentValues values) {
		return values.getAsInteger(Categories.CATEGORY_ID);
	}

	@Override
	protected String getPath() {
		return BggContract.PATH_CATEGORIES;
	}

	@Override
	protected String getTable() {
		return Tables.CATEGORIES;
	}

	@Override
	protected String getType(Uri uri) {
		return Categories.CONTENT_TYPE;
	}
}
