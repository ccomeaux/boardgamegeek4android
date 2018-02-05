package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.*
import com.boardgamegeek.provider.BggContract
import java.text.DecimalFormat

abstract class Sorter(protected val context: Context) {
    private val doubleFormat = DecimalFormat("#.0")

    @get:StringRes
    protected abstract val descriptionId: Int

    /**
     * Gets the description to display in the UI when this sort is applied. Subclasses should set descriptionId
     * to control this value.
     */
    open val description: String
        get() = context.getString(descriptionId)

    /**
     * Gets the sort order clause to use in the query.
     */
    val orderByClause: String
        get() = if (sortColumn.isEmpty()) {
            defaultSort
        } else {
            val sortOrder = if (isSortDescending) "DESC" else "ASC"
            val collateNoCase = if (shouldCollate) BggContract.COLLATE_NOCASE else ""
            "$sortColumn $collateNoCase $sortOrder, $defaultSort"
        }

    protected open val sortColumn: String
        get() = ""

    /**
     * Whether this is sorting descending or ascending.
     */
    protected open val isSortDescending: Boolean
        get() = false

    protected open val shouldCollate: Boolean
        get() = false

    /**
     * The default sort order if the sort order isn't specified. Also applied as a secondary sort.
     */
    protected abstract val defaultSort: String

    /**
     * Get the names of the columns to add to the select projection. By default, this only includes the sort column.
     */
    open val columns: Array<String>
        get() = if (sortColumn.isEmpty()) emptyArray() else arrayOf(sortColumn)

    /**
     * The unique type.
     */
    abstract val type: Int

    /**
     * Get the text to display in the section header.
     */
    fun getHeaderText(cursor: Cursor?, position: Int): String {
        var text = ""
        if (cursor == null || position < 0) {
            return text
        }
        val pos = cursor.position
        if (cursor.moveToPosition(position)) {
            text = getHeaderText(cursor)
        }
        cursor.moveToPosition(pos)
        return text
    }

    /**
     * Get the text to display in the section header for the current position in the cursor.
     */
    protected open fun getHeaderText(cursor: Cursor): String {
        return ""
    }

    /**
     * Get the ID for the header at the specified position of the cursor (as required by adapters).
     */
    fun getHeaderId(cursor: Cursor?, position: Int): Long {
        var id: Long = 0
        if (cursor == null || position < 0) {
            return id
        }
        val pos = cursor.position
        if (cursor.moveToPosition(position)) {
            id = getHeaderId(cursor)
        }
        cursor.moveToPosition(pos)
        return id
    }

    protected open fun getHeaderId(cursor: Cursor): Long {
        return getHeaderText(cursor).hashCode().toLong()
    }

    protected fun getLong(cursor: Cursor, columnName: String): Long {
        return cursor.getLong(columnName)
    }

    @JvmOverloads protected fun getInt(cursor: Cursor, columnName: String, defaultValue: Int = 0): Int {
        return cursor.getInt(columnName, defaultValue)
    }

    @JvmOverloads protected fun getIntAsString(cursor: Cursor, columnName: String, defaultValue: String, treatZeroAsNull: Boolean = false): String {
        return cursor.getIntAsString(columnName, defaultValue, treatZeroAsNull)
    }

    protected fun getDoubleAsString(cursor: Cursor, columnName: String, defaultValue: String, treatZeroAsNull: Boolean, format: DecimalFormat?): String {
        return cursor.getDoubleAsString(columnName, defaultValue, treatZeroAsNull, format ?: doubleFormat)
    }

    protected fun getFirstChar(cursor: Cursor, columnName: String): String {
        return cursor.getFirstChar(columnName)
    }

    @JvmOverloads protected fun getString(cursor: Cursor, columnName: String, defaultValue: String? = null): String? {
        return cursor.getString(columnName, defaultValue ?: "")
    }
}