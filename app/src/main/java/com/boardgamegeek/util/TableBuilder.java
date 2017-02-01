package com.boardgamegeek.util;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * A builder for creating and replacing tables.
 */
public class TableBuilder {
	private String tableName = null;
	private Column primaryKey = null;
	private List<Column> columns = new ArrayList<>();
	private List<String> uniqueColumnNames = new ArrayList<>();
	private CONFLICT_RESOLUTION resolution = CONFLICT_RESOLUTION.IGNORE;
	private boolean isFtsTable = false;

	public TableBuilder reset() {
		tableName = null;
		primaryKey = null;
		columns = new ArrayList<>();
		uniqueColumnNames = new ArrayList<>();
		resolution = CONFLICT_RESOLUTION.IGNORE;
		return this;
	}

	public void create(SQLiteDatabase db) {
		if (TextUtils.isEmpty(tableName)) {
			throw new IllegalStateException("Table not specified");
		}
		if (primaryKey == null) {
			throw new IllegalStateException("Primary key not specified");
		}
		StringBuilder sb = new StringBuilder();
		if (isFtsTable) {
			sb.append("CREATE VIRTUAL TABLE ").append(tableName).append(" USING fts3");
		} else {
			sb.append("CREATE TABLE ").append(tableName);
		}
		sb.append(" (").append(primaryKey.build()).append(" PRIMARY KEY AUTOINCREMENT,");
		for (Column column : columns) {
			sb.append(column.build()).append(",");
		}
		if (uniqueColumnNames.size() > 0) {
			sb.append("UNIQUE (");
			for (int i = 0; i < uniqueColumnNames.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(uniqueColumnNames.get(i));
			}
			sb.append(") ON CONFLICT ").append(resolution);
		} else {
			// remove comma
			sb = new StringBuilder(sb.substring(0, sb.length() - 1));
		}
		sb.append(")");
		Timber.d(sb.toString());
		db.execSQL(sb.toString());
	}

	public void replace(SQLiteDatabase db) {
		replace(db, null, null, null);
	}

	public void replace(SQLiteDatabase db, Map<String, String> columnMap, String joinTable, String joinColumn) {
		if (TextUtils.isEmpty(tableName)) {
			throw new IllegalStateException("Table not specified");
		}
		db.beginTransaction();
		try {
			rename(db);
			create(db);
			copy(db, columnMap, joinTable, joinColumn);
			dropTemp(db);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private String tempTable() {
		return tableName + "_tmp";
	}

	private void rename(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + tableName + " RENAME TO " + tempTable());
	}

	private void copy(SQLiteDatabase db, Map<String, String> columnMap, String joinTable, String joinColumn) {
		StringBuilder sourceColumns = new StringBuilder();
		StringBuilder destinationColumns = new StringBuilder();
		for (Column column : columns) {
			if (destinationColumns.length() > 0) destinationColumns.append(",");
			destinationColumns.append(column.name);

			if (sourceColumns.length() > 0) sourceColumns.append(",");
			String c = columnMap == null ? null : columnMap.get(column.name);
			if (TextUtils.isEmpty(c)) {
				sourceColumns.append(column.name);
			} else {
				sourceColumns.append(c);
			}
		}
		String destinationTable = tempTable();
		if (!TextUtils.isEmpty(joinTable) && !TextUtils.isEmpty(joinColumn))
			destinationTable += String.format(" INNER JOIN %1$s ON %1$s.%3$s=%2$s.%3$s", joinTable, tempTable(), joinColumn);
		String sql = String.format("INSERT INTO %s(%s) SELECT %s FROM %s",
			tableName, destinationColumns.toString(), sourceColumns.toString(), destinationTable);
		Timber.d(sql);
		db.execSQL(sql);
	}

	private void dropTemp(SQLiteDatabase db) {
		db.execSQL("DROP TABLE " + tempTable());
	}

	public TableBuilder setTable(String table) {
		tableName = table;
		isFtsTable = false;
		return this;
	}

	public TableBuilder setFtsTable(String table) {
		tableName = table;
		isFtsTable = true;
		return this;
	}

	public TableBuilder setConflictResolution(CONFLICT_RESOLUTION resolution) {
		this.resolution = resolution;
		return this;
	}

	public TableBuilder setPrimaryKey(String columnName, COLUMN_TYPE type) {
		primaryKey = new Column();
		primaryKey.name = columnName;
		primaryKey.type = type;
		return this;
	}

	/**
	 * Add an _ID column and sets it as the primary key.
	 */
	public TableBuilder useDefaultPrimaryKey() {
		primaryKey = new Column();
		primaryKey.name = BaseColumns._ID;
		primaryKey.type = COLUMN_TYPE.INTEGER;
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
		columns.add(c);

		if (unique) {
			if (!notNull) {
				throw new IllegalStateException("Unique columns must be non-null");
			}
			uniqueColumnNames.add(name);
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
