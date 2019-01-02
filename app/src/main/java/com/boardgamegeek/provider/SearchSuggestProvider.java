package com.boardgamegeek.provider;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.collection.ArrayMap;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;

import java.util.Locale;
import java.util.Map;

public class SearchSuggestProvider extends BaseProvider {
	public static final Map<String, String> sSuggestionProjectionMap = buildSuggestionProjectionMap();
	private static final String GROUP_BY = Collection.COLLECTION_NAME + "," + Collection.COLLECTION_YEAR_PUBLISHED;

	private static ArrayMap<String, String> buildSuggestionProjectionMap() {
		ArrayMap<String, String> map = new ArrayMap<>();
		map.put(BaseColumns._ID, BaseColumns._ID);
		mapAs(map, SearchManager.SUGGEST_COLUMN_TEXT_1, Collection.COLLECTION_NAME);
		mapAs(map, SearchManager.SUGGEST_COLUMN_TEXT_2, String.format("IFNULL(CASE WHEN %1$s=0 THEN NULL ELSE %1$s END, '?')", Collection.COLLECTION_YEAR_PUBLISHED));
		mapAs(map, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, Tables.COLLECTION + "." + Collection.GAME_ID);
		mapAs(map, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, Tables.COLLECTION + "." + Collection.COLLECTION_NAME);
		mapAs(map, SearchManager.SUGGEST_COLUMN_ICON_1, String.format("'%s/' || %s.%s || '/%s'", Games.CONTENT_URI, Tables.COLLECTION, Collection.GAME_ID, BggContract.PATH_THUMBNAILS));
		// mapAs(map, SearchManager.SUGGEST_COLUMN_ICON_2, String.format("'android.resource://com.boardgamegeek/%s'", R.drawable.ic_top_games));
		return map;
	}

	private static void mapAs(ArrayMap<String, String> map, String columnName, String query) {
		map.put(columnName, query + " AS " + columnName);
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
	protected Cursor query(ContentResolver resolver, SQLiteDatabase db, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		String query = null;
		if (uri.getPathSegments().size() > 1) {
			query = uri.getLastPathSegment().toLowerCase(Locale.US);
		}

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(Tables.COLLECTION);
		qb.setProjectionMap(sSuggestionProjectionMap);
		if (!TextUtils.isEmpty(query)) {
			qb.appendWhere(String.format("(%s.%s like '%s%%' OR %s.%s like '%% %s%%')",
				Tables.COLLECTION, Collection.COLLECTION_NAME, query, Tables.COLLECTION, Collection.COLLECTION_NAME, query));
		}
		Cursor cursor = qb.query(db, projection, selection, selectionArgs, GROUP_BY, null, getSortOrder(sortOrder), uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT));
		cursor.setNotificationUri(resolver, uri);
		return cursor;
	}

	@Override
	protected String getDefaultSortOrder() {
		return Collection.DEFAULT_SORT;
	}
}
