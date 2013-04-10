package com.boardgamegeek.provider;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggDatabase.Tables;

public class SearchRefreshProvider extends BaseProvider {

	@Override
	protected String getPath() {
		return SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/#";
	}

	@Override
	protected String getType(Uri uri) {
		return SearchManager.SHORTCUT_MIME_TYPE;
	}

	@Override
	protected Cursor query(ContentResolver resolver, SQLiteDatabase db, Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String shortcutId = null;
		if (uri.getPathSegments().size() > 1) {
			shortcutId = uri.getLastPathSegment();
		}
		if (TextUtils.isEmpty(shortcutId)) {
			return null;
		} else {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(Tables.GAMES);
			qb.setProjectionMap(SearchSuggestProvider.sSuggestionProjectionMap);
			qb.appendWhere(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID + "=" + shortcutId);
			Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
			cursor.setNotificationUri(resolver, uri);
			return cursor;
		}
	}
}
