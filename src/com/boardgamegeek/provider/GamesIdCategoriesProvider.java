package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.GamesCategories;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdCategoriesProvider extends BaseProvider {
	private static final String TABLE = Tables.GAMES_CATEGORIES;

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		final int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.GAMES_CATEGORIES_JOIN_CATEGORIES)
				.mapToTable(Categories._ID, Tables.CATEGORIES).mapToTable(Categories.CATEGORY_ID, Tables.CATEGORIES)
				.whereEquals(Tables.GAMES_CATEGORIES + "." + GamesCategories.GAME_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(TABLE).whereEquals(GamesCategories.GAME_ID, gameId);
	}

	@Override
	protected String getPath() {
		return "games/#/categories";
	}

	@Override
	protected String getType(Uri uri) {
		return Categories.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		long rowId = insert(db, uri, values, TABLE);
		return Games.buildCategoryUri(rowId);
	}
}
