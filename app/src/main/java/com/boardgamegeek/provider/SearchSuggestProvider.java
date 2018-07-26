package com.boardgamegeek.provider;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggDatabase.Tables;

import java.util.Locale;
import java.util.Map;

public class SearchSuggestProvider extends BaseProvider {
	public static final Map<String, String> sSuggestionProjectionMap = buildSuggestionProjectionMap();
	private static final String GROUP_BY = Collection.COLLECTION_NAME + ","
		+ Collection.COLLECTION_YEAR_PUBLISHED;

	private static ArrayMap<String, String> buildSuggestionProjectionMap() {
		ArrayMap<String, String> map = new ArrayMap<>();
		map.put(BaseColumns._ID, BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1,
			String.format("%s AS %s", Collection.COLLECTION_NAME, SearchManager.SUGGEST_COLUMN_TEXT_1));
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2,
			String.format("IFNULL(CASE WHEN %s=0 THEN NULL ELSE %s END, '?') AS %s", Collection.COLLECTION_YEAR_PUBLISHED, Collection.COLLECTION_YEAR_PUBLISHED, SearchManager.SUGGEST_COLUMN_TEXT_2));
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
			String.format("%s.%s AS %s", Tables.COLLECTION, Collection.GAME_ID, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID));
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
			String.format("%s.%s AS %s", Tables.COLLECTION, Collection.COLLECTION_NAME, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA));
		//		map.put(SearchManager.SUGGEST_COLUMN_ICON_2, "'" + Games.CONTENT_URI + "/' || " + Tables.COLLECTION + "."
		//			+ Collection.GAME_ID + " || '/" + BggContract.PATH_THUMBNAILS + "'" + " AS "
		//			+ SearchManager.SUGGEST_COLUMN_ICON_2);
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
