package com.boardgamegeek.util

import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import androidx.core.database.sqlite.transaction
import com.boardgamegeek.extensions.joinTo
import timber.log.Timber

/**
 * A builder for creating and replacing tables.
 */
class TableBuilder {
    private var tableName: String? = null
    private var primaryKey: Column? = null
    private var primaryKeyAutoincrement = false
    private var columns = mutableListOf<Column>()
    private var uniqueColumnNames = mutableListOf<String>()
    private var resolution = ConflictResolution.IGNORE
    private var isFtsTable = false

    fun reset(): TableBuilder {
        tableName = null
        primaryKey = null
        columns = mutableListOf()
        uniqueColumnNames = mutableListOf()
        resolution = ConflictResolution.IGNORE
        return this
    }

    fun create(db: SQLiteDatabase) {
        check(tableName?.isNotEmpty() == true) { "Table not specified" }
        val sb = StringBuilder()
        if (isFtsTable) {
            sb.append("CREATE VIRTUAL TABLE $tableName USING fts3")
        } else {
            sb.append("CREATE TABLE $tableName")
        }
        checkNotNull(primaryKey) { "Primary key not specified" }.let {
            sb.append(" (${it.build()} PRIMARY KEY")
            if (primaryKeyAutoincrement) sb.append(" AUTOINCREMENT")
            sb.append(",")
        }
        sb.append(columns.joinToString(", ") { it.build() })
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
        db.transaction {
            execSQL("ALTER TABLE $tableName RENAME TO ${tempTable()}")
            create(this)
            copy(this, columnMap, joinTable, joinColumn)
            execSQL("DROP TABLE ${tempTable()}")
        }
    }

    private fun tempTable() = tableName + "_tmp"

    private fun copy(db: SQLiteDatabase, columnMap: Map<String, String>?, joinTable: String?, joinColumn: String?) {
        val destinationColumns = columns.joinToString(",")
        val sourceColumns = columns.map {
            columnMap?.get(it.name)?.let { mappedColumn ->
                mappedColumn.ifBlank { it.name }
            } ?: it.name
        }.joinToString(",")
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

    @Suppress("unused")
    fun setFtsTable(table: String?) = apply {
        tableName = table
        isFtsTable = true
    }

    fun setConflictResolution(resolution: ConflictResolution) = apply {
        this.resolution = resolution
    }

    fun setPrimaryKey(columnName: String, type: ColumnType, autoincrement: Boolean = false) = apply {
        primaryKey = Column(columnName, type, notNull = true)
        primaryKeyAutoincrement = autoincrement
    }

    /**
     * Add an _ID column and sets it as the primary key.
     */
    fun useDefaultPrimaryKey() = apply {
        setPrimaryKey(BaseColumns._ID, ColumnType.INTEGER)
        primaryKeyAutoincrement = false
    }

    fun addColumn(name: String, type: ColumnType?, notNull: Boolean, defaultValue: Int) =
        addColumn(name, type, notNull, defaultValue = defaultValue.toString())

    fun addColumn(
        name: String,
        type: ColumnType?,
        notNull: Boolean = false,
        unique: Boolean = false,
        referenceTable: String? = null,
        referenceColumn: String? = null,
        onCascadeDelete: Boolean = false,
        defaultValue: String? = null,
    ) = apply {
        val column = Column(name, type, notNull, onCascadeDelete, defaultValue)
        column.setReference(referenceTable, referenceColumn)
        columns.add(column)
        if (unique) {
            check(notNull) { "Unique columns must be non-null" }
            uniqueColumnNames.add(name)
        }
    }

    enum class ColumnType {
        INTEGER, TEXT, REAL
    }

    @Suppress("unused")
    enum class ConflictResolution {
        ROLLBACK, ABORT, FAIL, IGNORE, REPLACE
    }

    private inner class Column(
        val name: String? = null,
        val type: ColumnType? = null,
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
