package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesCategoriesIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long id = ContentUris.parseId(uri);
		return new SelectionBuilder().table(Tables.GAMES_CATEGORIES).whereEquals(BaseColumns._ID, id);
	}

	@Override
	protected String getPath() {
		return "games/categories/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Categories.CONTENT_ITEM_TYPE;
	}
}
