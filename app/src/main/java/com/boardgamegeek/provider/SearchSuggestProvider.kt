package com.boardgamegeek.provider

import android.app.SearchManager
import android.content.ContentResolver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.Companion.PATH_THUMBNAILS
import com.boardgamegeek.provider.BggDatabase.Tables
import java.util.*

/**
 * Called from the search widget to populate the drop down list.
 * content://com.boardgamegeek/search_suggest_query/keyword?limit=50
 */
open class SearchSuggestProvider : BaseProvider() {
    override fun getType(uri: Uri) = SearchManager.SUGGEST_MIME_TYPE

    override val path = SearchManager.SUGGEST_URI_PATH_QUERY

    override val defaultSortOrder = Collection.DEFAULT_SORT

    override fun query(
        resolver: ContentResolver,
        db: SQLiteDatabase,
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val searchTerm = uri.lastPathSegment?.lowercase(Locale.getDefault()).orEmpty()
        val limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)
        val qb = SQLiteQueryBuilder().apply {
            tables = Tables.COLLECTION_JOIN_GAMES
            projectionMap = suggestionProjectionMap
            appendWhere("(${Collection.Columns.COLLECTION_NAME} like '$searchTerm%%' OR ${Collection.Columns.COLLECTION_NAME} like '%% $searchTerm%%')")
        }
        return qb.query(db, projection, selection, selectionArgs, GROUP_BY, null, getSortOrder(sortOrder), limit).apply {
            setNotificationUri(resolver, uri)
        }
    }

    companion object {
        private const val GROUP_BY = "${Collection.Columns.COLLECTION_NAME}, ${Collection.Columns.COLLECTION_YEAR_PUBLISHED}"

        @Suppress("SpellCheckingInspection")
        val suggestionProjectionMap = mutableMapOf(
            BaseColumns._ID to "${Tables.GAMES}.${BaseColumns._ID}",
            SearchManager.SUGGEST_COLUMN_TEXT_1 to "${Collection.Columns.COLLECTION_NAME} AS ${SearchManager.SUGGEST_COLUMN_TEXT_1}",
            SearchManager.SUGGEST_COLUMN_TEXT_2 to "IFNULL(CASE WHEN ${Collection.Columns.COLLECTION_YEAR_PUBLISHED}=0 THEN NULL ELSE ${Collection.Columns.COLLECTION_YEAR_PUBLISHED} END, '?') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2}",
            SearchManager.SUGGEST_COLUMN_ICON_2 to "'${Games.CONTENT_URI}/' || ${Tables.COLLECTION}.${Collection.Columns.GAME_ID} || '/$PATH_THUMBNAILS' AS ${SearchManager.SUGGEST_COLUMN_ICON_2}",
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID to "${Tables.GAMES}.${Games.Columns.GAME_ID} AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID}",
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA to "${Collection.Columns.COLLECTION_NAME} AS ${SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA}",
            SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT to "${Games.Columns.LAST_VIEWED} AS ${SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT}",
        )
    }
}
