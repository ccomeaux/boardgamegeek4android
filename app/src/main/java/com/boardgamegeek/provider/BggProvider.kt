package com.boardgamegeek.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.collection.SimpleArrayMap
import java.io.FileNotFoundException

class BggProvider : ContentProvider() {
    private lateinit var openHelper: BggDatabase

    override fun onCreate(): Boolean {
        openHelper = BggDatabase(context)
        return true
    }

    override fun getType(uri: Uri): String {
        return getProvider(uri)?.getType(uri) ?: throw UnsupportedOperationException("Unknown uri getting type: $uri")
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return getProvider(uri)?.query(
            openHelper.readableDatabase,
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Unknown uri inserting: $uri")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Unknown uri updating: $uri")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Unknown uri deleting: $uri")
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return context?.let { getProvider(uri)?.openFile(it, openHelper.readableDatabase, uri, mode) }
    }

    private fun getProvider(uri: Uri): BaseProvider? {
        val match = uriMatcher.match(uri)
        if (providers.containsKey(match)) {
            return providers[match]
        }
        throw UnsupportedOperationException("Unknown uri: $uri")
    }

    companion object {
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        private val providers = buildProviderMap()
        private var uriMatchCode = 1

        private fun buildProviderMap(): SimpleArrayMap<Int, BaseProvider> {
            val map = SimpleArrayMap<Int, BaseProvider>()
            addProvider(map, GamesIdProvider())
            addProvider(map, CollectionIdThumbnailProvider())
            addProvider(map, SearchSuggestProvider())
            return map
        }

        private fun addProvider(map: SimpleArrayMap<Int, BaseProvider>, provider: BaseProvider) {
            uriMatchCode++
            uriMatcher.addURI(BggContract.CONTENT_AUTHORITY, provider.path, uriMatchCode)
            map.put(uriMatchCode, provider)
        }
    }
}
