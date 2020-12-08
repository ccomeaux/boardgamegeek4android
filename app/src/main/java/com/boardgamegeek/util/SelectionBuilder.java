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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import timber.log.Timber;

/**
 * Helper for building selection clauses for {@link SQLiteDatabase}. Each appended clause is combined using {@code AND}.
 * This class is <em>not</em> thread safe. Borrowed, then expanded, from com.google.android.apps.iosched.util.
 */
public class SelectionBuilder {
	private String tableName = null;
	private final Map<String, String> projectionMap = new ArrayMap<>();
	private final StringBuilder selection = new StringBuilder();
	private final List<String> selectionArgs = new ArrayList<>();
	private final List<String> groupBy = new ArrayList<>();
	private String having = null;
	private String limit = null;

	/**
	 * Reset any internal state, allowing this builder to be recycled.
	 */
	public SelectionBuilder reset() {
		tableName = null;
		projectionMap.clear();
		selection.setLength(0);
		selectionArgs.clear();
		groupBy.clear();
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
		if (this.selection.length() > 0) {
			this.selection.append(" AND ");
		}
		this.selection.append("(").append(selection).append(")");

		if (selectionArgs != null) {
			Collections.addAll(this.selectionArgs, selectionArgs);
		}

		return this;
	}

	public SelectionBuilder table(String table) {
		tableName = table;
		return this;
	}

	public SelectionBuilder limit(String rowCount) {
		limit = null;
		if (rowCount != null) {
			int count = StringUtils.parseInt(rowCount, 0);
			if (count > 0) {
				limit = rowCount;
			}
		}
		return this;
	}

	private void assertTable() {
		if (tableName == null) {
			throw new IllegalStateException("Table not specified");
		}
	}

	private void assertHaving() {
		if (!TextUtils.isEmpty(having) && (groupBy.size() == 0)) {
			throw new IllegalStateException("Group by must be specified for Having clause");
		}
	}

	public SelectionBuilder mapToTable(String column, String table) {
		if (column.equals(BaseColumns._ID)) {
			return mapToTable(column, table, column);
		} else {
			projectionMap.put(column, String.format("%s.%s", table, column));
		}
		return this;
	}

	public SelectionBuilder mapToTable(String column, String table, String alias) {
		projectionMap.put(column, String.format("%s.%s AS %s", table, column, alias));
		return this;
	}

	public SelectionBuilder mapIfNull(String column, String nullDefault) {
		projectionMap.put(column, String.format("IFNULL(%s,%s) AS %s", column, nullDefault, column));
		return this;
	}

	public SelectionBuilder mapIfNullToTable(String column, String table, String nullDefault) {
		projectionMap.put(column, String.format("IFNULL(%s.%s,%s) AS %s", table, column, nullDefault, column));
		return this;
	}

	public SelectionBuilder mapAsSum(String aliasColumn, String sumColumn) {
		return map(aliasColumn, String.format("SUM(%s)", sumColumn));
	}

	public SelectionBuilder mapAsSum(String aliasColumn, String sumColumn, String table) {
		return map(aliasColumn, String.format("SUM(%s.%s)", table, sumColumn));
	}

	public SelectionBuilder mapAsMax(String aliasColumn, String maxColumn) {
		return map(aliasColumn, String.format("MAX(%s)", maxColumn));
	}

	public SelectionBuilder map(String fromColumn, String toClause) {
		projectionMap.put(fromColumn, String.format("%s AS %s", toClause, fromColumn));
		return this;
	}

	public SelectionBuilder mapAsCount(String fromColumn) {
		projectionMap.put(fromColumn, String.format("count(*) AS %s", fromColumn));
		return this;
	}

	public SelectionBuilder groupBy(String... groupArgs) {
		groupBy.clear();
		if (groupArgs != null) {
			Collections.addAll(groupBy, groupArgs);
		}
		return this;
	}

	public SelectionBuilder having(String having) {
		this.having = having;
		return this;
	}

	/**
	 * Return selection string for current internal state.
	 *
	 * @see #getSelectionArgs()
	 */
	public String getSelection() {
		return selection.toString();
	}

	/**
	 * Return selection arguments for current internal state.
	 *
	 * @see #getSelection()
	 */
	public String[] getSelectionArgs() {
		return selectionArgs.toArray(new String[selectionArgs.size()]);
	}

	public String getGroupByClause() {
		if (groupBy.size() == 0) {
			return "";
		}
		StringBuilder clause = new StringBuilder();
		for (String arg : groupBy) {
			if (clause.length() > 0) {
				clause.append(", ");
			}
			final String target = projectionMap.get(arg);
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
		if (columns == null) return;
		if (columns.length == 0) return;
		for (int i = 0; i < columns.length; i++) {
			final String target = projectionMap.get(columns[i]);
			if (target != null) columns[i] = target;
		}
	}

	@NotNull
	@Override
	public String toString() {
		return "table=[" + tableName + "], selection=[" + getSelection() + "], selectionArgs="
			+ Arrays.toString(getSelectionArgs()) + ", groupBy=[" + getGroupByClause() + "], having=[" + having + "]";
	}

	/**
	 * Execute query using the current internal state as {@code WHERE} clause.
	 */
	public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
		assertHaving();
		return query(db, columns, getGroupByClause(), having, orderBy, limit);
	}

	/**
	 * Execute query using the current internal state as {@code WHERE} clause.
	 */
	public Cursor query(SQLiteDatabase db, String[] columns, String groupBy, String having, String orderBy, String limit) {
		assertTable();
		mapColumns(columns);
		Timber.v("QUERY: columns=%s, %s", Arrays.toString(columns), this);
		Cursor c = db.query(tableName, columns, getSelection(), getSelectionArgs(), groupBy, having, orderBy, limit);
		Timber.v("queried %,d rows", c.getCount());
		return c;
	}

	/**
	 * Execute update using the current internal state as {@code WHERE} clause.
	 */
	public int update(SQLiteDatabase db, ContentValues values) {
		assertTable();
		Timber.v("UPDATE: %s", this);
		int count = db.update(tableName, values, getSelection(), getSelectionArgs());
		Timber.v("updated %,d rows", count);
		return count;
	}

	/**
	 * Execute delete using the current internal state as {@code WHERE} clause.
	 */
	public int delete(SQLiteDatabase db) {
		assertTable();
		Timber.v("DELETE: %s", this);
		String selection = getSelection();
		if (TextUtils.isEmpty(selection)) {
			// this forces delete to return the count
			selection = "1";
		}
		int count = db.delete(tableName, selection, getSelectionArgs());
		Timber.v("deleted %,d rows", count);
		return count;
	}

	public static String whereNullOrEmpty(String columnName) {
		return String.format("(%1$S IS NULL OR %1$S='')", columnName);
	}

	@NonNull
	public static String whereZeroOrNull(String columnName) {
		return String.format("(%1$s=0 OR %1$s IS NULL)", columnName);
	}
}
