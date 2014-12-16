package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggDatabase.GamesCategories;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdCategoriesIdProvider extends BaseProvider {
	GamesIdCategoriesProvider mProvider = new GamesIdCategoriesProvider();

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long categoryId = ContentUris.parseId(uri);
		return mProvider.buildSimpleSelection(uri).whereEquals(GamesCategories.CATEGORY_ID, categoryId);
	}

	@Override
	protected String getPath() {
		return addIdToPath(mProvider.getPath());
	}

	@Override
	protected String getType(Uri uri) {
		return Categories.CONTENT_ITEM_TYPE;
	}
}
