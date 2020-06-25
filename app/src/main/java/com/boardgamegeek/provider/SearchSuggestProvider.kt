package com.boardgamegeek.provider

import android.app.SearchManager
import android.content.ContentResolver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.CollectionColumns.COLLECTION_NAME
import com.boardgamegeek.provider.BggContract.CollectionColumns.COLLECTION_YEAR_PUBLISHED
import com.boardgamegeek.provider.BggContract.GamesColumns.GAME_ID
import com.boardgamegeek.provider.BggContract.GamesColumns.LAST_VIEWED
import com.boardgamegeek.provider.BggContract.PATH_THUMBNAILS
import com.boardgamegeek.provider.BggDatabase.Tables
import java.util.*

/**
 * Called from the search widget to populate the drop down list.
 * content://com.boardgamegeek/search_suggest_query/catan?limit=50
 */
open class SearchSuggestProvider : BaseProvider() {
    override fun getType(uri: Uri) = SearchManager.SUGGEST_MIME_TYPE

    override val path = SearchManager.SUGGEST_URI_PATH_QUERY

    override val defaultSortOrder = BggContract.Collection.DEFAULT_SORT

    override fun query(resolver: ContentResolver, db: SQLiteDatabase, uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val searchTerm = uri.lastPathSegment?.toLowerCase(Locale.getDefault()).orEmpty()
        val limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT)
        val qb = SQLiteQueryBuilder().apply {
            tables = Tables.COLLECTION_JOIN_GAMES
            setProjectionMap(suggestionProjectionMap)
            appendWhere("($COLLECTION_NAME like '$searchTerm%%' OR $COLLECTION_NAME like '%% $searchTerm%%')")
        }
        return qb.query(db, projection, selection, selectionArgs, GROUP_BY, null, getSortOrder(sortOrder), limit).apply {
            setNotificationUri(resolver, uri)
        }
    }

    companion object {
        private const val GROUP_BY = "$COLLECTION_NAME, $COLLECTION_YEAR_PUBLISHED"

        val suggestionProjectionMap = mutableMapOf(
                BaseColumns._ID to "${Tables.GAMES}.${BaseColumns._ID}",
                SearchManager.SUGGEST_COLUMN_TEXT_1 to "$COLLECTION_NAME AS ${SearchManager.SUGGEST_COLUMN_TEXT_1}",
                SearchManager.SUGGEST_COLUMN_TEXT_2 to "IFNULL(CASE WHEN $COLLECTION_YEAR_PUBLISHED=0 THEN NULL ELSE $COLLECTION_YEAR_PUBLISHED END, '?') AS ${SearchManager.SUGGEST_COLUMN_TEXT_2}",
                SearchManager.SUGGEST_COLUMN_ICON_2 to "'${BggContract.Games.CONTENT_URI}/' || ${Tables.COLLECTION}.$GAME_ID || '/$PATH_THUMBNAILS' AS ${SearchManager.SUGGEST_COLUMN_ICON_2}",
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID to "${Tables.GAMES}.$GAME_ID AS ${SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID}",
                SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA to "$COLLECTION_NAME AS ${SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA}",
                SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT to "$LAST_VIEWED AS ${SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT}"
        )
    }
}
