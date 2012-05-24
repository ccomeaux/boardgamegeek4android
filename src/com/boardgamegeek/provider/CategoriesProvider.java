package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CategoriesProvider extends BaseProvider {
	
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.CATEGORIES);
	}

	@Override
	protected String getPath() {
		return "categories";
	}

	@Override
	protected String getType(Uri uri) {
		return Categories.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		if (db.insertOrThrow(Tables.CATEGORIES, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Categories.buildCategoryUri(values.getAsInteger(Categories.CATEGORY_ID));
	}
}
