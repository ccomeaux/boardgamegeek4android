package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.util.SelectionBuilder;

public abstract class BaseProvider {
	
	protected abstract String getPath();

	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return buildSimpleSelection(uri);
	}

	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}

	protected void deleteChildren(final SQLiteDatabase db, final SelectionBuilder builder) {
		return;
	}

	protected String getType(Uri uri) {
		throw new UnsupportedOperationException("Unknown uri getting type: " + uri);
	}

	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		throw new UnsupportedOperationException("Unknown uri inserting: " + uri);
	}
}
