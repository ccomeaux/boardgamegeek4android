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
package com.boardgamegeek.provider

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import timber.log.Timber

@Suppress("SpellCheckingInspection")
/**
 * Helper for building selection clauses for [SQLiteDatabase]. Each appended clause is combined using `AND`.
 * This class is *not* thread safe. Borrowed, then expanded, from com.google.android.apps.iosched.util.
 */
class SelectionBuilder {
    private var tableName: String? = null
    private val projectionMap = mutableMapOf<String, String>()
    private var selection = ""
    private val selectionArgs = mutableListOf<String>()
    private val groupBy = mutableListOf<String>()
    private var limit: String? = null

    /**
     * Reset any internal state, allowing this builder to be recycled.
     */
    fun reset(): SelectionBuilder {
        tableName = null
        projectionMap.clear()
        selection = ""
        selectionArgs.clear()
        groupBy.clear()
        limit = null
        return this
    }

    fun whereEquals(column: String, selectionArg: String?) = where("$column=?", selectionArg)

    fun whereEquals(column: String, selectionArg: Int) = where("$column=?", selectionArg.toString())

    fun whereEquals(column: String, selectionArg: Long) = where("$column=?", selectionArg.toString())

    fun whereEqualsOrNull(column: String, selectionArg: String?) = where("$column=? OR $column IS NULL", selectionArg)

    /**
     * Append the given selection clause to the internal state. Each clause is surrounded with parenthesis and combined
     * using `AND`.
     */
    fun where(selection: String?, vararg selectionArgs: String?): SelectionBuilder {
        if (selection == null || selection.isEmpty()) {
            require(selectionArgs.isEmpty()) { "Valid selection required when including arguments=" }
            // Shortcut when clause is empty
            return this
        }

        // TODO: map selection similar to projection
        if (this.selection.isNotBlank()) {
            this.selection += " AND "
        }
        this.selection += "($selection)"
        this.selectionArgs.addAll(selectionArgs.asList().filterNotNull())
        return this
    }

    fun table(table: String?): SelectionBuilder {
        tableName = table
        return this
    }

    fun limit(rowCount: String?): SelectionBuilder {
        limit = null
        rowCount?.let {
            it.toIntOrNull()?.let { count -> if (count > 0) limit = it }
        }
        return this
    }

    private fun assertTable() {
        checkNotNull(tableName) { "Table not specified" }
    }

    fun map(fromColumn: String, toClause: String?): SelectionBuilder {
        projectionMap[fromColumn] = "$toClause AS $fromColumn"
        return this
    }

    fun mapToTable(column: String, table: String?): SelectionBuilder {
        return if (column == BaseColumns._ID) {
            mapToTable(column, table, column)
        } else {
            projectionMap[column] = "$table.$column"
            this
        }
    }

    fun mapToTable(column: String, table: String?, alias: String?): SelectionBuilder {
        projectionMap[column] = "$table.$column AS $alias"
        return this
    }

    @Suppress("SpellCheckingInspection")
    fun mapIfNull(column: String, nullDefault: String, table: String? = null) = map(column, "IFNULL(${column.withTable(table)},$nullDefault)")

    fun mapAsSum(aliasColumn: String, sumColumn: String, table: String? = null) = map(aliasColumn, "SUM(${sumColumn.withTable(table)})")

    fun mapAsMax(aliasColumn: String, maxColumn: String) = map(aliasColumn, "MAX($maxColumn)")

    fun mapAsCount(fromColumn: String) = map(fromColumn, "COUNT(*)")

    private fun String.withTable(table: String?): String = if (table == null) this else "$table.$this"

    fun groupBy(vararg groupArgs: String?): SelectionBuilder {
        groupBy.clear()
        groupBy.addAll(groupArgs.asList().filterNotNull())
        return this
    }

    /**
     * Return selection arguments for current internal state.
     *
     * @see .getSelection
     */
    private fun getSelectionArgs(): Array<String> {
        return selectionArgs.toTypedArray()
    }

    private fun getGroupByClause(): String {
        return groupBy.joinToString(", ") {
            projectionMap[it]?.let { projection ->
                if (!projection.contains(" AS ")) {
                    projection
                } else it
            } ?: it
        }
    }

    private fun mapColumns(columns: Array<String>?): Array<String>? {
        return columns?.map { column ->
            projectionMap[column] ?: column
        }?.toTypedArray()
    }

    override fun toString(): String {
        return ("table=[$tableName], selection=[$selection], selectionArgs=${getSelectionArgs().contentToString()}, groupBy=[${getGroupByClause()}]")
    }

    /**
     * Execute query using the current internal state as `WHERE` clause.
     */
    fun query(db: SQLiteDatabase, columns: Array<String>?, orderBy: String?): Cursor {
        return query(db, columns, getGroupByClause(), null, orderBy, limit)
    }

    /**
     * Execute query using the current internal state as `WHERE` clause.
     */
    fun query(db: SQLiteDatabase, columns: Array<String>?, groupBy: String?, having: String?, orderBy: String?, limit: String?): Cursor {
        assertTable()
        val mappedColumns = mapColumns(columns)
        Timber.v("QUERY: columns=${mappedColumns.contentToString()}, $this")
        val cursor = db.query(tableName, mappedColumns, selection, getSelectionArgs(), groupBy, having, orderBy, limit)
        Timber.v("queried %,d rows", cursor.count)
        return cursor
    }

    /**
     * Execute update using the current internal state as `WHERE` clause.
     */
    fun update(db: SQLiteDatabase, values: ContentValues?): Int {
        assertTable()
        Timber.v("UPDATE: %s", this)
        val count = db.update(tableName, values, selection, getSelectionArgs())
        Timber.v("updated %,d rows", count)
        return count
    }

    /**
     * Execute delete using the current internal state as `WHERE` clause.
     */
    fun delete(db: SQLiteDatabase): Int {
        assertTable()
        Timber.v("DELETE: %s", this)
        val selection = selection.ifEmpty { "1" } // this forces delete to return the count
        val count = db.delete(tableName, selection, getSelectionArgs())
        Timber.v("deleted %,d rows", count)
        return count
    }
}
