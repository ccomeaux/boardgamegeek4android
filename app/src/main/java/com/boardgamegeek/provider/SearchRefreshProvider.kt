package com.boardgamegeek.provider

import android.app.SearchManager
import android.content.ContentResolver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.boardgamegeek.provider.BggDatabase.Tables

class SearchRefreshProvider : BaseProvider() {
    override fun getType(uri: Uri) = SearchManager.SHORTCUT_MIME_TYPE

    override val path = "${SearchManager.SUGGEST_URI_PATH_SHORTCUT}/#"

    override fun query(resolver: ContentResolver, db: SQLiteDatabase, uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val shortcutId = uri.lastPathSegment
        return if (shortcutId.isNullOrBlank()) {
            null
        } else {
            val qb = SQLiteQueryBuilder()
            qb.tables = Tables.GAMES
            qb.setProjectionMap(SearchSuggestProvider.suggestionProjectionMap)
            qb.appendWhere(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID + "=" + shortcutId)
            qb.query(db, projection, selection, selectionArgs, null, null, sortOrder).apply {
                setNotificationUri(resolver, uri)
            }
        }
    }
}