package com.boardgamegeek.ui.adapter

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.support.v4.widget.SimpleCursorAdapter
import com.boardgamegeek.R

/**
 * A simple adapter to use for [android.widget.AutoCompleteTextView].
 */
class AutoCompleteAdapter @JvmOverloads constructor(
        private val context: Context,
        private val columnName: String,
        private val uri: Uri,
        private val sortOrder: String? = null,
        private val extraColumnName: String? = null
) : SimpleCursorAdapter(
        context,
        R.layout.autocomplete_item,
        null,
        arrayOf(BaseColumns._ID, columnName, extraColumnName),
        intArrayOf(0, R.id.autocomplete_item),
        0) {

    override fun getStringConversionColumn() = 1

    override fun runQueryOnBackgroundThread(constraint: CharSequence): Cursor? {
        return context.contentResolver.query(uri,
                arrayOf(BaseColumns._ID, columnName, extraColumnName),
                if (constraint.isEmpty()) null else "$columnName LIKE ?",
                if (constraint.isEmpty()) null else arrayOf(constraint.toString() + "%"),
                sortOrder)
    }
}
