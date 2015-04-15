/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boardgamegeek.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Helper for building selection clauses for {@link SQLiteDatabase}. Each appended clause is combined using {@code AND}.
 * This class is <em>not</em> thread safe. Borrowed, then expanded, from com.google.android.apps.iosched.util.
 */
public class SelectionBuilder {
	private String mTable = null;
	private final Map<String, String> mProjectionMap = new HashMap<>();
	private final StringBuilder mSelection = new StringBuilder();
	private final List<String> mSelectionArgs = new ArrayList<>();
	private final List<String> mGroupBy = new ArrayList<>();
	private String mHaving = null;
	private String mLimit = null;

	/**
	 * Reset any internal state, allowing this builder to be recycled.
	 */
	public SelectionBuilder reset() {
		mTable = null;
		mProjectionMap.clear();
		mSelection.setLength(0);
		mSelectionArgs.clear();
		mGroupBy.clear();
		return this;
	}

	public SelectionBuilder whereEquals(String column, String selectionArg) {
		return where(column + "=?", selectionArg);
	}

	public SelectionBuilder whereEquals(String column, int selectionArg) {
		return where(column + "=?", String.valueOf(selectionArg));
	}

	public SelectionBuilder whereEquals(String column, long selectionArg) {
		return where(column + "=?", String.valueOf(selectionArg));
	}

	public SelectionBuilder whereEqualsOrNull(String column, String selectionArg) {
		return where(column + "=? OR " + column + " IS NULL", selectionArg);
	}

	public SelectionBuilder whereEqualsOrNull(String column, int selectionArg) {
		return where(column + "=? OR " + column + " IS NULL", String.valueOf(selectionArg));
	}

	public SelectionBuilder whereEqualsOrNull(String column, long selectionArg) {
		return where(column + "=? OR " + column + " IS NULL", String.valueOf(selectionArg));
	}

	/**
	 * Append the given selection clause to the internal state. Each clause is surrounded with parenthesis and combined
	 * using {@code AND}.
	 */
	public SelectionBuilder where(String selection, String... selectionArgs) {
		if (TextUtils.isEmpty(selection)) {
			if (selectionArgs != null && selectionArgs.length > 0) {
				throw new IllegalArgumentException("Valid selection required when including arguments=");
			}

			// Shortcut when clause is empty
			return this;
		}

		// TODO: map selection similar to projection
		if (mSelection.length() > 0) {
			mSelection.append(" AND ");
		}
		mSelection.append("(").append(selection).append(")");

		if (selectionArgs != null) {
			Collections.addAll(mSelectionArgs, selectionArgs);
		}

		return this;
	}

	public SelectionBuilder table(String table) {
		mTable = table;
		return this;
	}

	public SelectionBuilder limit(String rowCount) {
		int count = StringUtils.parseInt(rowCount, 0);
		if (count > 0) {
			mLimit = rowCount;
		} else {
			mLimit = null;
		}
		return this;
	}

	private void assertTable() {
		if (mTable == null) {
			throw new IllegalStateException("Table not specified");
		}
	}

	private void assertHaving() {
		if (!TextUtils.isEmpty(mHaving) && (mGroupBy == null || mGroupBy.size() == 0)) {
			throw new IllegalStateException("Group by must be specified for Having clause");
		}
	}

	public SelectionBuilder mapToTable(String column, String table) {
		if (column.equals(BaseColumns._ID)) {
			mapToTable(column, table, column);
		} else {
			mProjectionMap.put(column, table + "." + column);
		}
		return this;
	}

	public SelectionBuilder mapToTable(String column, String table, String fromColumn) {
		mProjectionMap.put(column, table + "." + column + " AS " + fromColumn);
		return this;
	}

	public SelectionBuilder mapToTable(String column, String table, String fromColumn, String nullDefault) {
		mProjectionMap.put(column, "IFNULL(" + table + "." + column + "," + nullDefault + ") AS " + fromColumn);
		return this;
	}

	public SelectionBuilder map(String fromColumn, String toClause) {
		mProjectionMap.put(fromColumn, toClause + " AS " + fromColumn);
		return this;
	}

	public SelectionBuilder groupBy(String... groupArgs) {
		mGroupBy.clear();
		if (groupArgs != null) {
			Collections.addAll(mGroupBy, groupArgs);
		}
		return this;
	}

	public SelectionBuilder having(String having) {
		mHaving = having;
		return this;
	}

	/**
	 * Return selection string for current internal state.
	 *
	 * @see #getSelectionArgs()
	 */
	public String getSelection() {
		return mSelection.toString();
	}

	/**
	 * Return selection arguments for current internal state.
	 *
	 * @see #getSelection()
	 */
	public String[] getSelectionArgs() {
		return mSelectionArgs.toArray(new String[mSelectionArgs.size()]);
	}

	public String getGroupByClause() {
		if (mGroupBy == null || mGroupBy.size() == 0) {
			return "";
		}
		StringBuilder clause = new StringBuilder();
		for (String arg : mGroupBy) {
			if (clause.length() > 0) {
				clause.append(", ");
			}
			final String target = mProjectionMap.get(arg);
			if (target != null) {
				if (!target.contains(" AS ")) {
					arg = target;
				}
			}
			clause.append(arg);
		}
		return clause.toString();
	}

	private void mapColumns(String[] columns) {
		for (int i = 0; i < columns.length; i++) {
			final String target = mProjectionMap.get(columns[i]);
			if (target != null) {
				columns[i] = target;
			}
		}
	}

	@Override
	public String toString() {
		return "table=[" + mTable + "], selection=[" + getSelection() + "], selectionArgs="
			+ Arrays.toString(getSelectionArgs()) + ", groupBy=[" + getGroupByClause() + "], having=[" + mHaving + "]";
	}

	/**
	 * Execute query using the current internal state as {@code WHERE} clause.
	 */
	public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
		assertHaving();
		return query(db, columns, getGroupByClause(), mHaving, orderBy, mLimit);
	}

	/**
	 * Execute query using the current internal state as {@code WHERE} clause.
	 */
	public Cursor query(SQLiteDatabase db, String[] columns, String groupBy, String having, String orderBy, String limit) {
		assertTable();
		if (columns != null) {
			mapColumns(columns);
		}
		Timber.v("QUERY: columns=" + Arrays.toString(columns) + ", " + this);
		Cursor c = db.query(mTable, columns, getSelection(), getSelectionArgs(), groupBy, having, orderBy, limit);
		Timber.v("queried " + c.getCount() + " rows");
		return c;
	}

	/**
	 * Execute update using the current internal state as {@code WHERE} clause.
	 */
	public int update(SQLiteDatabase db, ContentValues values) {
		assertTable();
		Timber.v("UPDATE: " + this);
		int count = db.update(mTable, values, getSelection(), getSelectionArgs());
		Timber.v("updated " + count + " rows");
		return count;
	}

	/**
	 * Execute delete using the current internal state as {@code WHERE} clause.
	 */
	public int delete(SQLiteDatabase db) {
		assertTable();
		Timber.v("DELETE: " + this);
		String selection = getSelection();
		if (TextUtils.isEmpty(selection)) {
			// this forces delete to return the count
			selection = "1";
		}
		int count = db.delete(mTable, selection, getSelectionArgs());
		Timber.v("deleted " + count + " rows");
		return count;
	}
}
