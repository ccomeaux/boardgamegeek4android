package com.boardgamegeek.util;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * A builder for creating and replacing tables.
 */
public class TableBuilder {
	private String mTable = null;
	private Column mPrimaryKey = null;
	private List<Column> mColumns = new ArrayList<>();
	private List<String> mUniqueColumnNames = new ArrayList<>();
	private CONFLICT_RESOLUTION mResolution = CONFLICT_RESOLUTION.IGNORE;
	private boolean mFtsTable = false;

	public TableBuilder reset() {
		mTable = null;
		mPrimaryKey = null;
		mColumns = new ArrayList<>();
		mUniqueColumnNames = new ArrayList<>();
		mResolution = CONFLICT_RESOLUTION.IGNORE;
		return this;
	}

	public void create(SQLiteDatabase db) {
		if (TextUtils.isEmpty(mTable)) {
			throw new IllegalStateException("Table not specified");
		}
		if (mPrimaryKey == null) {
			throw new IllegalStateException("Primary key not specified");
		}
		String table = mFtsTable ? "CREATE VIRTUAL TABLE " + mTable + " USING fts3" : "CREATE TABLE " + mTable;
		StringBuilder sb = new StringBuilder();
		sb.append(table).append(" (").append(mPrimaryKey.build()).append(" PRIMARY KEY AUTOINCREMENT,");
		for (Column column : mColumns) {
			sb.append(column.build()).append(",");
		}
		if (mUniqueColumnNames.size() > 0) {
			sb.append("UNIQUE (");
			for (int i = 0; i < mUniqueColumnNames.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(mUniqueColumnNames.get(i));
			}
			sb.append(") ON CONFLICT ").append(mResolution);
		} else {
			// remove comma
			sb = new StringBuilder(sb.substring(0, sb.length() - 1));
		}
		sb.append(")");
		Timber.d(sb.toString());
		db.execSQL(sb.toString());
	}

	public void replace(SQLiteDatabase db) {
		if (TextUtils.isEmpty(mTable)) {
			throw new IllegalStateException("Table not specified");
		}
		db.beginTransaction();
		try {
			rename(db);
			create(db);
			copy(db);
			dropTemp(db);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private String tempTable() {
		return mTable + "_tmp";
	}

	private void rename(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + mTable + " RENAME TO " + tempTable());
	}

	private void copy(SQLiteDatabase db) {
		StringBuilder sb = new StringBuilder();
		for (Column column : mColumns) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(column.name);
		}
		String sql = "INSERT INTO " + mTable + "(" + sb.toString() + ") SELECT " + sb.toString() + " FROM "
			+ tempTable();
		Timber.d(sql);
		db.execSQL(sql);
	}

	private void dropTemp(SQLiteDatabase db) {
		db.execSQL("DROP TABLE " + tempTable());
	}

	public TableBuilder setTable(String table) {
		mTable = table;
		mFtsTable = false;
		return this;
	}

	public TableBuilder setFtsTable(String table) {
		mTable = table;
		mFtsTable = true;
		return this;
	}

	public TableBuilder setConflictResolution(CONFLICT_RESOLUTION resolution) {
		mResolution = resolution;
		return this;
	}

	public TableBuilder setPrimaryKey(String columnName, COLUMN_TYPE type) {
		mPrimaryKey = new Column();
		mPrimaryKey.name = columnName;
		mPrimaryKey.type = type;
		return this;
	}

	/**
	 * Add an _ID column and sets it as the primary key.
	 */
	public TableBuilder useDefaultPrimaryKey() {
		mPrimaryKey = new Column();
		mPrimaryKey.name = BaseColumns._ID;
		mPrimaryKey.type = COLUMN_TYPE.INTEGER;
		return this;
	}

	public TableBuilder addColumn(String name, COLUMN_TYPE type) {
		return addColumn(name, type, false, false);
	}

	public TableBuilder addColumn(String name, COLUMN_TYPE type, boolean notNull) {
		return addColumn(name, type, notNull, false);
	}

	public TableBuilder addColumn(String name, COLUMN_TYPE type, boolean notNull, int defaultValue) {
		return addColumn(name, type, notNull, false, null, null, false, String.valueOf(defaultValue));
	}

	public TableBuilder addColumn(String name, COLUMN_TYPE type, boolean notNull, boolean unique) {
		return addColumn(name, type, notNull, unique, null, null);
	}

	public TableBuilder addColumn(String name, COLUMN_TYPE type, boolean notNull, boolean unique,
								  String referenceTable, String referenceColumn) {
		return addColumn(name, type, notNull, unique, referenceTable, referenceColumn, false, null);
	}

	public TableBuilder addColumn(String name, COLUMN_TYPE type, boolean notNull, boolean unique,
								  String referenceTable, String referenceColumn, boolean onCascadeDelete) {
		return addColumn(name, type, notNull, unique, referenceTable, referenceColumn, onCascadeDelete, null);
	}

	TableBuilder addColumn(String name, COLUMN_TYPE type, boolean notNull, boolean unique,
						   String referenceTable, String referenceColumn, boolean onCascadeDelete, String defaultValue) {
		Column c = new Column();
		c.name = name;
		c.type = type;
		c.notNull = notNull;
		c.setReference(referenceTable, referenceColumn);
		c.onCascadeDelete = onCascadeDelete;
		c.defaultValue = defaultValue;
		mColumns.add(c);

		if (unique) {
			if (!notNull) {
				throw new IllegalStateException("Unique columns must be non-null");
			}
			mUniqueColumnNames.add(name);
		}

		return this;
	}

	public enum COLUMN_TYPE {
		INTEGER, TEXT, REAL
	}

	public enum CONFLICT_RESOLUTION {
		ROLLBACK, ABORT, FAIL, IGNORE, REPLACE
	}

	private class Column {
		String name;
		COLUMN_TYPE type;
		boolean notNull;
		private String refTable;
		private String refColumn;
		private boolean onCascadeDelete;
		private String defaultValue;

		private void setReference(String table, String column) {
			if (TextUtils.isEmpty(table) && !TextUtils.isEmpty(column) || TextUtils.isEmpty(column)
				&& !TextUtils.isEmpty(table)) {
				throw new IllegalStateException("Table and column must be specified");
			}
			refTable = table;
			refColumn = column;
		}

		String build() {
			// "NAME TEXT NOT NULL REFERENCES PARENT(NAME) ON DELETE CASCADE"
			String s = name + " " + type;
			if (notNull) {
				s += " NOT NULL";
			}
			if (!TextUtils.isEmpty(defaultValue)) {
				s += " DEFAULT " + defaultValue + " ";
			}
			if (refTable != null) {
				s += " REFERENCES " + refTable + "(" + refColumn + ")";
			}
			if (onCascadeDelete) {
				s += " ON DELETE CASCADE";
			}
			return s;
		}
	}
}
