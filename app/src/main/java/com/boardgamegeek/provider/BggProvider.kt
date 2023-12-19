package com.boardgamegeek.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.collection.SimpleArrayMap
import androidx.core.content.contentValuesOf
import java.io.FileNotFoundException

class BggProvider : ContentProvider() {
    private lateinit var openHelper: BggDatabase

    private fun reqContext(): Context {
        return context ?: throw IllegalStateException("Cannot find context from the provider.")
    }

    override fun onCreate(): Boolean {
        openHelper = BggDatabase(context)
        return true
    }

    override fun getType(uri: Uri): String {
        return getProvider(uri)?.getType(uri) ?: throw UnsupportedOperationException("Unknown uri getting type: $uri")
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return getProvider(uri)?.query(
            reqContext().contentResolver,
            openHelper.readableDatabase,
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.also {
            it.setNotificationUri(reqContext().contentResolver, uri)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return getProvider(uri)?.insert(reqContext(), openHelper.writableDatabase, uri, values ?: contentValuesOf())?.also {
            context?.contentResolver?.notifyChange(it, null)
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return getProvider(uri)?.update(reqContext(), openHelper.writableDatabase, uri, values, selection, selectionArgs) ?: 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return getProvider(uri)?.delete(
            reqContext(),
            openHelper.writableDatabase,
            uri,
            selection,
            selectionArgs
        ) ?: 0
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return getProvider(uri)?.openFile(reqContext(), uri, mode)
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
            addProvider(map, ThumbnailsProvider())
            addProvider(map, ThumbnailsIdProvider())
            addProvider(map, GamesIdThumbnailProvider())
            addProvider(map, CollectionIdThumbnailProvider())
            addProvider(map, AvatarsProvider())
            addProvider(map, UsersUsernameAvatarProvider())
            addProvider(map, SearchSuggestProvider())
            addProvider(map, SearchSuggestTextProvider())
            addProvider(map, SearchRefreshProvider())
            return map
        }

        private fun addProvider(map: SimpleArrayMap<Int, BaseProvider>, provider: BaseProvider) {
            uriMatchCode++
            uriMatcher.addURI(BggContract.CONTENT_AUTHORITY, provider.path, uriMatchCode)
            map.put(uriMatchCode, provider)
        }
    }
}
