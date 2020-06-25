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

    private fun requireContext(): Context {
        return this.context
                ?: throw IllegalStateException("ContentProvider $this not attached to a context.")
    }

    override fun onCreate(): Boolean {
        openHelper = BggDatabase(context)
        return true
    }

    override fun getType(uri: Uri): String {
        return getProvider(uri)?.getType(uri)
                ?: throw UnsupportedOperationException("Unknown uri getting type: $uri")
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return getProvider(uri)?.query(requireContext().contentResolver, openHelper.readableDatabase, uri, projection, selection, selectionArgs, sortOrder)?.also {
            it.setNotificationUri(requireContext().contentResolver, uri)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return getProvider(uri)?.insert(requireContext(), openHelper.writableDatabase, uri,
                values ?: contentValuesOf())?.also {
            context?.contentResolver?.notifyChange(it, null)
        }
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return getProvider(uri)?.update(requireContext(), openHelper.writableDatabase, uri, values, selection, selectionArgs)
                ?: 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return getProvider(uri)?.delete(requireContext(),
                openHelper.writableDatabase,
                uri, selection, selectionArgs)
                ?: 0
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return getProvider(uri)?.openFile(requireContext(), uri, mode)
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
            addProvider(map, GamesProvider())
            addProvider(map, GamesIdProvider())
            addProvider(map, GamesIdRankProvider())
            addProvider(map, GamesIdRankIdProvider())
            addProvider(map, GamesIdExpansionsProvider())
            addProvider(map, GamesIdExpansionsIdProvider())
            addProvider(map, GamesIdDesignersProvider())
            addProvider(map, GamesIdDesignersIdProvider())
            addProvider(map, GamesIdArtistsProvider())
            addProvider(map, GamesIdArtistsIdProvider())
            addProvider(map, GamesIdPublishersProvider())
            addProvider(map, GamesIdPublishersIdProvider())
            addProvider(map, GamesIdCategoriesProvider())
            addProvider(map, GamesIdCategoriesIdProvider())
            addProvider(map, GamesIdMechanicsProvider())
            addProvider(map, GamesIdMechanicsIdProvider())
            addProvider(map, GamesRanksProvider())
            addProvider(map, GamesRanksIdProvider())
            addProvider(map, GamesDesignersIdProvider())
            addProvider(map, GamesArtistsIdProvider())
            addProvider(map, GamesPublishersIdProvider())
            addProvider(map, GamesMechanicsIdProvider())
            addProvider(map, GamesCategoriesIdProvider())
            addProvider(map, GamesIdSuggestedPlayerCountPollResultsProvider())
            addProvider(map, GamesIdSuggestedPlayerCountPollResultProvider())
            addProvider(map, GamesIdPollsProvider())
            addProvider(map, GamesIdPollsNameProvider())
            addProvider(map, GamesIdPollsNameResultsProvider())
            addProvider(map, GamesIdPollsNameResultsResultProvider())
            addProvider(map, GamesIdPollsNameResultsKeyProvider())
            addProvider(map, GamesIdPollsNameResultsKeyResultProvider())
            addProvider(map, GamesIdPollsNameResultsKeyResultKeyProvider())
            addProvider(map, GamesIdColorsProvider())
            addProvider(map, GamesIdColorsNameProvider())
            addProvider(map, GamesIdPlaysProvider())
            addProvider(map, DesignersProvider())
            addProvider(map, DesignersIdProvider())
            addProvider(map, DesignersIdCollectionProvider())
            addProvider(map, ArtistsProvider())
            addProvider(map, ArtistsIdProvider())
            addProvider(map, ArtistsIdCollectionProvider())
            addProvider(map, PublishersProvider())
            addProvider(map, PublishersIdProvider())
            addProvider(map, PublishersIdCollectionProvider())
            addProvider(map, MechanicsProvider())
            addProvider(map, MechanicsIdProvider())
            addProvider(map, MechanicsIdCollectionProvider())
            addProvider(map, CategoriesProvider())
            addProvider(map, CategoriesIdProvider())
            addProvider(map, CategoriesIdCollectionProvider())
            addProvider(map, CollectionProvider())
            addProvider(map, CollectionIdProvider())
            addProvider(map, CollectionAcquiredFromProvider())
            addProvider(map, CollectionInventoryLocationProvider())
            addProvider(map, PlaysProvider())
            addProvider(map, PlaysIdProvider())
            addProvider(map, PlaysIdPlayersProvider())
            addProvider(map, PlaysIdPlayersIdProvider())
            addProvider(map, PlaysLocationsProvider())
            addProvider(map, PlaysPlayersProvider())
            addProvider(map, CollectionViewProvider())
            addProvider(map, CollectionViewIdProvider())
            addProvider(map, CollectionViewIdFiltersProvider())
            addProvider(map, CollectionViewIdFiltersIdProvider())
            addProvider(map, BuddiesProvider())
            addProvider(map, BuddiesIdProvider())
            addProvider(map, ThumbnailsProvider())
            addProvider(map, ThumbnailsIdProvider())
            addProvider(map, GamesIdThumbnailProvider())
            addProvider(map, CollectionIdThumbnailProvider())
            addProvider(map, AvatarsProvider())
            addProvider(map, BuddiesIdAvatarProvider())
            addProvider(map, SearchSuggestProvider())
            addProvider(map, SearchSuggestTextProvider())
            addProvider(map, SearchRefreshProvider())
            addProvider(map, PlayerColorsProvider())
            addProvider(map, UsersNameColorsProvider())
            addProvider(map, UsersNameColorsOrderProvider())
            addProvider(map, PlayersNameColorsProvider())
            addProvider(map, PlayersNameColorsOrderProvider())
            return map
        }

        private fun addProvider(map: SimpleArrayMap<Int, BaseProvider>, provider: BaseProvider) {
            uriMatchCode++
            uriMatcher.addURI(BggContract.CONTENT_AUTHORITY, provider.path, uriMatchCode)
            map.put(uriMatchCode, provider)
        }
    }
}