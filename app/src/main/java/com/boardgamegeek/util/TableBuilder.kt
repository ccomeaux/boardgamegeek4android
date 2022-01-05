package com.boardgamegeek.util

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.boardgamegeek.extensions.joinTo
import timber.log.Timber

/**
 * A builder for creating and replacing tables.
 */
class TableBuilder {
    private var tableName: String? = null
    private var primaryKey: Column? = null
    private var columns = mutableListOf<Column>()
    private var uniqueColumnNames = mutableListOf<String>()
    private var resolution = CONFLICT_RESOLUTION.IGNORE
    private var isFtsTable = false

    fun reset(): TableBuilder {
        tableName = null
        primaryKey = null
        columns = ArrayList()
        uniqueColumnNames = ArrayList()
        resolution = CONFLICT_RESOLUTION.IGNORE
        return this
    }

    fun create(db: SQLiteDatabase) {
        check(tableName?.isNotEmpty() == true) { "Table not specified" }
        checkNotNull(primaryKey) { "Primary key not specified" }
        val sb = StringBuilder()
        if (isFtsTable) {
            sb.append("CREATE VIRTUAL TABLE $tableName USING fts3")
        } else {
            sb.append("CREATE TABLE $tableName")
        }
        sb.append(" (").append(primaryKey!!.build()).append(" PRIMARY KEY AUTOINCREMENT,")
        columns.joinToString(",")
        if (uniqueColumnNames.isNotEmpty()) {
            sb.append(", UNIQUE (")
            sb.append(uniqueColumnNames.joinTo(","))
            sb.append(") ON CONFLICT ").append(resolution)
        }
        sb.append(")")
        Timber.d(sb.toString())
        db.execSQL(sb.toString())
    }

    fun replace(db: SQLiteDatabase, columnMap: Map<String, String>? = null, joinTable: String? = null, joinColumn: String? = null) {
        check(tableName?.isNotEmpty() == true) { "Table not specified" }
        db.beginTransaction()
        try {
            db.execSQL("ALTER TABLE $tableName RENAME TO ${tempTable()}")
            create(db)
            copy(db, columnMap, joinTable, joinColumn)
            db.execSQL("DROP TABLE ${tempTable()}")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun tempTable() = tableName + "_tmp"

    private fun copy(db: SQLiteDatabase, columnMap: Map<String, String>?, joinTable: String?, joinColumn: String?) {
        val sourceColumns = StringBuilder()
        val destinationColumns = StringBuilder()
        for (column in columns) {
            if (destinationColumns.isNotEmpty()) destinationColumns.append(",")
            destinationColumns.append(column.name)
            if (sourceColumns.isNotEmpty()) sourceColumns.append(",")
            val c = columnMap?.get(column.name)
            if (c?.isNotBlank() == true) {
                sourceColumns.append(c)
            } else {
                sourceColumns.append(column.name)
            }
        }
        var destinationTable = tempTable()
        if (joinTable?.isNotEmpty() == true && joinColumn?.isNotEmpty() == true) destinationTable += " INNER JOIN $joinTable ON $joinTable.$joinColumn=${tempTable()}.$joinColumn"
        val sql = "INSERT INTO $tableName($destinationColumns) SELECT $sourceColumns FROM $destinationTable"
        Timber.d(sql)
        db.execSQL(sql)
    }

    fun setTable(table: String?) = apply {
        tableName = table
        isFtsTable = false
    }

    fun setFtsTable(table: String?) = apply {
        tableName = table
        isFtsTable = true
    }

    fun setConflictResolution(resolution: CONFLICT_RESOLUTION) = apply {
        this.resolution = resolution
    }

    fun setPrimaryKey(columnName: String, type: COLUMN_TYPE) = apply {
        primaryKey = Column(columnName, type)
    }

    /**
     * Add an _ID column and sets it as the primary key.
     */
    fun useDefaultPrimaryKey() = apply {
        primaryKey = Column(BaseColumns._ID, COLUMN_TYPE.INTEGER)
    }

    fun addColumn(name: String, type: COLUMN_TYPE?, notNull: Boolean, defaultValue: Int) =
        addColumn(name, type, notNull, defaultValue = defaultValue.toString())

    fun addColumn(
        name: String, type: COLUMN_TYPE?, notNull: Boolean = false, unique: Boolean = false,
        referenceTable: String? = null, referenceColumn: String? = null, onCascadeDelete: Boolean = false, defaultValue: String? = null
    ) = apply {
        val column = Column(name, type, notNull, onCascadeDelete, defaultValue)
        column.setReference(referenceTable, referenceColumn)
        columns.add(column)
        if (unique) {
            check(notNull) { "Unique columns must be non-null" }
            uniqueColumnNames.add(name)
        }
    }

    enum class COLUMN_TYPE {
        INTEGER, TEXT, REAL
    }

    enum class CONFLICT_RESOLUTION {
        ROLLBACK, ABORT, FAIL, IGNORE, REPLACE
    }

    private inner class Column(
        val name: String? = null,
        val type: COLUMN_TYPE? = null,
        val notNull: Boolean = false,
        val onCascadeDelete: Boolean = false,
        val defaultValue: String? = null,
    ) {
        private var refTable: String? = null
        private var refColumn: String? = null

        fun setReference(table: String?, column: String?) {
            check(
                !((table.isNullOrBlank() && !column.isNullOrBlank()) ||
                        (column.isNullOrBlank() && !table.isNullOrBlank()))
            ) { "Table and column must be specified" }
            refTable = table
            refColumn = column
        }

        fun build(): String {
            // "COLUMN_NAME TEXT NOT NULL DEFAULT DEFAULT_VALUE REFERENCES PARENT(NAME) ON DELETE CASCADE"
            var s = "$name $type"
            if (notNull) s += " NOT NULL"
            if (defaultValue?.isNotEmpty() == true) s += " DEFAULT $defaultValue "
            if (refTable != null) s += " REFERENCES $refTable($refColumn)"
            if (onCascadeDelete) s += " ON DELETE CASCADE"
            return s
        }
    }
}
