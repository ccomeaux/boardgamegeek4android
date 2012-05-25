package com.boardgamegeek.provider;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.util.SelectionBuilder;

public abstract class BaseProvider {

	protected abstract String getPath();

	protected String addIdToPath(String path) {
		return path + "/#";
	}

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

	protected long insert(SQLiteDatabase db, Uri uri, ContentValues values, String mTable) {
		long rowId = db.insertOrThrow(mTable, null, values);
		if (rowId == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return rowId;
	}

	protected int queryInt(SQLiteDatabase db, SelectionBuilder builder, String columnName) {
		int id = 0;
		Cursor cursor = builder.query(db, new String[] { columnName }, null);
		try {
			if (cursor.moveToFirst()) {
				id = cursor.getInt(0);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return id;
	}

	protected List<String> getList(final SQLiteDatabase db, final SelectionBuilder builder, String columnName) {
		List<String> list = new ArrayList<String>();
		Cursor c = builder.query(db, new String[] { columnName }, null);
		try {
			if (c.moveToNext()) {
				list.add(c.getString(0));
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return list;
	}
}
