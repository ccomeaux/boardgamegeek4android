package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.buildBasicUri
import com.boardgamegeek.util.SelectionBuilder

abstract class BasicProvider : BaseProvider() {
    protected abstract val table: String?

    public override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder().table(table)
    }

    protected open val insertedIdColumn: String?
        get() = null

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val rowId = db.insert(table, null, values)
        return if (rowId != -1L) {
            insertedUri(values, rowId)
        } else null
    }

    protected open fun insertedUri(values: ContentValues?, rowId: Long): Uri? {
        return if (insertedIdColumn.isNullOrBlank())
            buildBasicUri(path, rowId)
        else {
            values?.getAsLong(insertedIdColumn)?.let {
                buildBasicUri(path, it)
            }
        }
    }
}
