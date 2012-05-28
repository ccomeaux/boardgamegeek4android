package com.boardgamegeek.provider;

import java.util.HashMap;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.provider.BggDatabase.Tables;

public class SearchSuggestProvider extends BaseProvider {

	public static final HashMap<String, String> sSuggestionProjectionMap = buildSuggestionProjectionMap();

	private static HashMap<String, String> buildSuggestionProjectionMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(Games._ID, Games._ID);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, Games.GAME_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2, Games.YEAR_PUBLISHED + " AS "
				+ SearchManager.SUGGEST_COLUMN_TEXT_2);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, Tables.GAMES + "." + Games.GAME_ID + " AS "
				+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, Tables.GAMES + "." + Games.GAME_ID + " AS "
				+ SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		map.put(SearchManager.SUGGEST_COLUMN_ICON_1, "0 AS " + SearchManager.SUGGEST_COLUMN_ICON_1);
		map.put(SearchManager.SUGGEST_COLUMN_ICON_2, "'" + Thumbnails.CONTENT_URI + "/' || " + Tables.GAMES + "."
				+ Games.THUMBNAIL_URL + " AS " + SearchManager.SUGGEST_COLUMN_ICON_2);
		map.put(Games.GAME_SORT_NAME, "(CASE WHEN " + Games.GAME_SORT_NAME + " IS NULL THEN " + Games.GAME_SORT_NAME
				+ " ELSE " + Games.GAME_SORT_NAME + " END) AS " + Games.GAME_SORT_NAME);
		return map;
	}

	@Override
	protected String getPath() {
		return SearchManager.SUGGEST_URI_PATH_QUERY;
	}

	@Override
	protected String getType(Uri uri) {
		return SearchManager.SUGGEST_MIME_TYPE;
	}

	@Override
	protected Cursor query(ContentResolver resolver, SQLiteDatabase db, Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String query = null;
		if (uri.getPathSegments().size() > 1) {
			query = uri.getLastPathSegment().toLowerCase();
		}

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(Tables.GAMES);
		qb.setProjectionMap(sSuggestionProjectionMap);
		if (!TextUtils.isEmpty(query)) {
			qb.appendWhere("(" + Tables.GAMES + "." + Games.GAME_NAME + " like '" + query + "%' OR " + Tables.GAMES
					+ "." + Games.GAME_NAME + " like '% " + query + "%')");
		}
		Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, getSortOrder(sortOrder),
				uri.getQueryParameter("limit"));
		cursor.setNotificationUri(resolver, uri);
		return cursor;
	}

	@Override
	protected String getDefaultSortOrder() {
		return Games.DEFAULT_SORT;
	}
}
