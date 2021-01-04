package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.contentValuesOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.export.Constants
import com.boardgamegeek.export.model.*
import com.boardgamegeek.extensions.applyBatch
import com.boardgamegeek.extensions.getIntOrNull
import com.boardgamegeek.extensions.load
import com.boardgamegeek.extensions.rowExists
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.jetbrains.anko.collections.forEachWithIndex
import timber.log.Timber
import java.io.*

class DataPortViewModel(application: Application) : AndroidViewModel(application) {
    val gson: Gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()

    private val _message = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>>
        get() = _message

    private val _collectionViewProgress = MutableLiveData<Pair<Int, Int>>()
    val collectionViewProgress: LiveData<Pair<Int, Int>>
        get() = _collectionViewProgress

    private val _gameProgress = MutableLiveData<Pair<Int, Int>>()
    val gameProgress: LiveData<Pair<Int, Int>>
        get() = _gameProgress

    private val _userProgress = MutableLiveData<Pair<Int, Int>>()
    val userProgress: LiveData<Pair<Int, Int>>
        get() = _userProgress

    fun exportCollectionViews(uri: Uri) {
        export(uri,
                Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION,
                1,
                _collectionViewProgress,
                {
                    getApplication<BggApplication>().contentResolver.load(
                            CollectionViews.CONTENT_URI,
                            arrayOf(
                                    CollectionViews._ID,
                                    CollectionViews.NAME,
                                    CollectionViews.SORT_TYPE,
                                    CollectionViews.STARRED
                            ))
                },
                { cursor: Cursor, writer: JsonWriter ->
                    val filters = mutableListOf<Filter>()
                    getApplication<BggApplication>().contentResolver.load(
                            CollectionViews.buildViewFilterUri(cursor.getLong(0)),
                            arrayOf(
                                    BggContract.CollectionViewFilters._ID,
                                    BggContract.CollectionViewFilters.TYPE,
                                    BggContract.CollectionViewFilters.DATA
                            )
                    )?.use {
                        while (it.moveToNext()) {
                            (it.getIntOrNull(1) ?: 0).also { type ->
                                if (type > 0) filters.add(Filter(type, it.getString(2).orEmpty()))
                            }
                        }
                    }

                    gson.toJson(CollectionView(
                            cursor.getString(1),
                            cursor.getInt(2),
                            cursor.getInt(3) == 1,
                            filters,
                    ), CollectionView::class.java, writer)
                })
    }

    fun exportGames(uri: Uri) {
        val context = getApplication<BggApplication>()
        export(
                uri,
                Constants.TYPE_GAMES_DESCRIPTION,
                1,
                _gameProgress,
                {
                    context.contentResolver.load(
                            BggContract.Games.CONTENT_URI,
                            arrayOf(BggContract.Games.GAME_ID))
                },
                { cursor: Cursor, writer: JsonWriter ->
                    val gameId = cursor.getInt(0)

                    val colors = mutableListOf<Color>()
                    context.contentResolver.load(
                            BggContract.Games.buildColorsUri(gameId),
                            arrayOf(BggContract.GameColors.COLOR)
                    )?.use {
                        while (it.moveToNext()) {
                            it.getString(0).orEmpty().also { color ->
                                if (color.isNotBlank()) colors.add(Color(color))
                            }
                        }
                    }

                    if (colors.isNotEmpty()) {
                        gson.toJson(Game(gameId, colors), Game::class.java, writer)
                    }
                },
        )
    }

    fun exportUsers(uri: Uri) {
        val context = getApplication<BggApplication>()
        export(
                uri,
                Constants.TYPE_USERS_DESCRIPTION,
                1,
                _userProgress,
                {
                    context.contentResolver.load(
                            BggContract.Buddies.CONTENT_URI,
                            arrayOf(BggContract.Buddies.BUDDY_NAME))
                },
                { cursor: Cursor, writer: JsonWriter ->
                    val name = cursor.getString(0)

                    val colors = mutableListOf<PlayerColor>()
                    context.contentResolver.load(
                            BggContract.PlayerColors.buildUserUri(name),
                            arrayOf(
                                    BggContract.PlayerColors._ID,
                                    BggContract.PlayerColors.PLAYER_COLOR_SORT_ORDER,
                                    BggContract.PlayerColors.PLAYER_COLOR
                            )
                    )?.use {
                        while (it.moveToNext()) {
                            it.getString(2).orEmpty().also { color ->
                                if (color.isNotBlank()) colors.add(PlayerColor(it.getInt(1), color))
                            }
                        }
                    }

                    if (colors.isNotEmpty()) {
                        gson.toJson(User(name, colors), User::class.java, writer)
                    }
                },
        )
    }

    private fun export(
            uri: Uri,
            typeDescription: String,
            version: Int,
            progress: MutableLiveData<Pair<Int, Int>>,
            getCursor: () -> Cursor?,
            writeJsonRecord: (cursor: Cursor, writer: JsonWriter) -> Unit,
    ) {
        val context = getApplication<BggApplication>()

        val cursor = getCursor()
        if (cursor == null) {
            postMessage(R.string.msg_export_failed_null_cursor)
            return
        }

        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "w")
        } catch (e: SecurityException) {
            Timber.w(e)
            postMessage(R.string.msg_export_failed_permissions, uri)
            return
        } catch (e: FileNotFoundException) {
            Timber.w(e)
            postMessage(R.string.msg_export_failed_file_not_found, uri)
            return
        }
        if (pfd == null) {
            postMessage(R.string.msg_export_failed_null_pfd, uri)
            return
        }

        getApplication<BggApplication>().appExecutors.diskIO.execute {
            val out: OutputStream = FileOutputStream(pfd.fileDescriptor)

            try {
                JsonWriter(OutputStreamWriter(out, "UTF-8")).use {
                    it.setIndent("  ")
                    it.beginObject()
                    it.name(Constants.NAME_TYPE).value(typeDescription)
                    it.name(Constants.NAME_VERSION).value(version)
                    it.name(Constants.NAME_ITEMS)
                    it.beginArray()

                    var numExported = 0
                    while (cursor.moveToNext()) {
                        progress.postValue(cursor.count to numExported++)
                        try {
                            writeJsonRecord(cursor, it)
                        } catch (e: RuntimeException) {
                            Timber.e(e)
                        }
                    }
                    progress.postValue(cursor.count to cursor.count)

                    it.endArray()
                    it.endObject()
                }
            } catch (e: Exception) {
                Timber.e(e)
                postMessage(R.string.msg_export_failed_write_json)
            } finally {
                cursor.close()
            }

            try {
                pfd.close()
            } catch (e: IOException) {
                Timber.w(e)
            }

            postMessage(R.string.msg_export_success)
        }
    }

    private fun postMessage(@StringRes resId: Int, vararg formatArgs: Any?) {
        _message.postValue(Event(getApplication<BggApplication>().getString(resId, *formatArgs)))
    }

    fun importCollectionViews(uri: Uri) {
        import(
                uri,
                Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION,
                _collectionViewProgress,
                { reader ->
                    gson.fromJson(reader, CollectionView::class.java)
                },
                { item: CollectionView, _ ->
                    val contentResolver = getApplication<BggApplication>().contentResolver
                    val values = contentValuesOf(
                            CollectionViews.NAME to item.name,
                            CollectionViews.STARRED to item.starred,
                            CollectionViews.SORT_TYPE to item.sortType,
                    )
                    val insertedUri = contentResolver.insert(CollectionViews.CONTENT_URI, values)
                    val viewId = CollectionViews.getViewId(insertedUri)
                    val filterUri = CollectionViews.buildViewFilterUri(viewId.toLong())
                    val batch = arrayListOf<ContentProviderOperation>()
                    for (filter in item.filters) {
                        val builder = ContentProviderOperation.newInsert(filterUri)
                                .withValue(BggContract.CollectionViewFilters.TYPE, filter.type)
                                .withValue(BggContract.CollectionViewFilters.DATA, filter.data)
                        batch.add(builder.build())
                    }
                    contentResolver.applyBatch(batch)
                },
                {
                    getApplication<BggApplication>().contentResolver.delete(CollectionViews.CONTENT_URI, null, null)
                },
        )
    }

    fun importGames(uri: Uri) {
        import(
                uri,
                Constants.TYPE_GAMES_DESCRIPTION,
                _gameProgress,
                { reader ->
                    gson.fromJson(reader, Game::class.java)
                },
                { item: Game, _ ->
                    val contentResolver = getApplication<BggApplication>().contentResolver
                    if (contentResolver.rowExists(BggContract.Games.buildGameUri(item.id))) {
                        val gameColorsUri = BggContract.Games.buildColorsUri(item.id)
                        contentResolver.delete(gameColorsUri, null, null)
                        val values = mutableListOf<ContentValues>()
                        item.colors.filter { it.color.isNotBlank() }.forEach { color ->
                            values.add(contentValuesOf(BggContract.GameColors.COLOR to color.color))
                        }
                        if (values.isNotEmpty()) {
                            contentResolver.bulkInsert(gameColorsUri, values.toTypedArray())
                        }
                    }
                },
        )
    }

    fun importUsers(uri: Uri) {
        import(
                uri,
                Constants.TYPE_USERS_DESCRIPTION,
                _userProgress,
                { reader: JsonReader ->
                    gson.fromJson(reader, User::class.java)
                },
                { item: User, _: Int ->
                    val contentResolver = getApplication<BggApplication>().contentResolver
                    if (contentResolver.rowExists(BggContract.Buddies.buildBuddyUri(item.name))) {
                        val batch = arrayListOf<ContentProviderOperation>()
                        item.colors.filter { it.color.isNotBlank() }.forEach { color ->
                            val builder = if (contentResolver.rowExists(BggContract.PlayerColors.buildUserUri(item.name, color.sort))) {
                                ContentProviderOperation
                                        .newUpdate(BggContract.PlayerColors.buildUserUri(item.name, color.sort))
                            } else {
                                ContentProviderOperation
                                        .newInsert(BggContract.PlayerColors.buildUserUri(item.name))
                                        .withValue(BggContract.PlayerColors.PLAYER_COLOR_SORT_ORDER, color.sort)
                            }
                            batch.add(builder.withValue(BggContract.PlayerColors.PLAYER_COLOR, color.color).build())
                        }
                        contentResolver.applyBatch(batch)
                    }
                },
        )
    }

    private fun <T> import(
            uri: Uri,
            typeDescription: String,
            progress: MutableLiveData<Pair<Int, Int>>,
            parseItem: (reader: JsonReader) -> T,
            importRecord: (item: T, version: Int) -> Unit,
            initializeImport: () -> Unit = {},
    ) {
        val context = getApplication<BggApplication>()
        val items: MutableList<T> = mutableListOf()

        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: SecurityException) {
            Timber.w(e)
            postMessage(R.string.msg_import_failed_file_not_read, uri)
            return
        } catch (e: FileNotFoundException) {
            Timber.w(e)
            postMessage(R.string.msg_import_failed_file_not_exist, uri)
            return
        }
        if (pfd == null) {
            postMessage(R.string.msg_export_failed_null_pfd, uri)
            return
        }

        var version = 0
        getApplication<BggApplication>().appExecutors.diskIO.execute {
            val reader = JsonReader(InputStreamReader(FileInputStream(pfd.fileDescriptor), "UTF-8"))
            try {
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        items += parseItem(reader)
                    }
                    reader.endArray()
                } else {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            Constants.NAME_TYPE -> reader.nextString().also {
                                if (it != typeDescription) {
                                    progress.postValue(items.size to items.size)
                                    postMessage(R.string.msg_import_failed_wrong_type, typeDescription, it!!)
                                    return@execute
                                }
                            }
                            Constants.NAME_VERSION -> version = reader.nextInt()
                            Constants.NAME_ITEMS -> {
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    items += parseItem(reader)
                                }
                                reader.endArray()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
            } catch (e: Exception) {
                progress.postValue(items.size to items.size)
                Timber.w(e, "Importing %s JSON file.", typeDescription)
                postMessage(R.string.msg_import_failed_parse_json)
            } finally {
                try {
                    reader.close()
                } catch (e: IOException) {
                    progress.postValue(items.size to items.size)
                    Timber.w(e, "Failed trying to close the JsonReader")
                }
            }

            initializeImport()
            items.forEachWithIndex { i, item ->
                progress.postValue(items.size to i)
                importRecord(item, version)
            }

            try {
                pfd.close()
            } catch (e: IOException) {
                Timber.w(e)
            }

            progress.postValue(items.size to items.size)
            postMessage(R.string.msg_import_success)
        }
    }
}