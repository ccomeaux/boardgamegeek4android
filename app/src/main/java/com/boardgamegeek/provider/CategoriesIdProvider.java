package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CategoriesIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int categoryId = Categories.getCategoryId(uri);
		return new SelectionBuilder().table(Tables.CATEGORIES).whereEquals(Categories.CATEGORY_ID, categoryId);
	}

	@Override
	protected String getPath() {
		return "categories/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Categories.CONTENT_ITEM_TYPE;
	}
}
