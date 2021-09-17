package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.StringRes
import androidx.core.content.contentValuesOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.export.Constants
import com.boardgamegeek.export.model.*
import com.boardgamegeek.extensions.applyBatch
import com.boardgamegeek.extensions.rowExists
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.mappers.mapToExportable
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.repository.CollectionViewRepository
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.repository.UserRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.collections.forEachWithIndex
import timber.log.Timber
import java.io.*

class DataPortViewModel(application: Application) : AndroidViewModel(application) {
    private val collectionViewRepository = CollectionViewRepository(getApplication())
    private val gameRepository = GameRepository(getApplication())
    private val userRepository = UserRepository(getApplication())
    private val playRepository = PlayRepository(getApplication())

    private val gson: Gson = GsonBuilder()
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
        viewModelScope.launch(Dispatchers.IO) {
            val views = collectionViewRepository.load(includeDefault = false, includeFilters = true).map  { it.mapToExportable() }
            export(
                uri,
                Constants.TYPE_COLLECTION_VIEWS_DESCRIPTION,
                1,
                _collectionViewProgress,
                views,
            ) { record: CollectionView, writer: JsonWriter ->
                gson.toJson(record, CollectionView::class.java, writer)
            }
        }
    }

    fun exportGames(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val colors = gameRepository.getPlayColors().map { Game(it.key, it.value.map { color -> Color(color) }) }.filter { it.colors.isNotEmpty() }
            export(
                uri,
                Constants.TYPE_GAMES_DESCRIPTION,
                1,
                _gameProgress,
                colors,
            ) { record: Game, writer: JsonWriter ->
                gson.toJson(record, Game::class.java, writer)
            }
        }
    }

    fun exportUsers(uri: Uri) {
        // TODO export non-users
        viewModelScope.launch(Dispatchers.IO) {
            val buddies = userRepository.loadAllUsers().map {
                val colors = playRepository.loadUserColors(it.userName).filter { color -> color.description.isNotBlank() }
                User(it.userName, colors.map { color -> PlayerColor(color.sortOrder, color.description) })
            }.filter { it.colors.isNotEmpty() }
            export(
                uri,
                Constants.TYPE_USERS_DESCRIPTION,
                1,
                _userProgress,
                buddies,
            ) { record: User, writer: JsonWriter ->
                gson.toJson(record, User::class.java, writer)
            }
        }
    }

    private suspend fun openFile(uri: Uri): ParcelFileDescriptor? = withContext(Dispatchers.IO) {
        val pfd = try {
            getApplication<BggApplication>().contentResolver.openFileDescriptor(uri, "w")
        } catch (e: SecurityException) {
            Timber.w(e)
            postMessage(R.string.msg_export_failed_permissions, uri)
            null
        } catch (e: FileNotFoundException) {
            Timber.w(e)
            postMessage(R.string.msg_export_failed_file_not_found, uri)
            null
        }
        if (pfd == null) {
            postMessage(R.string.msg_export_failed_null_pfd, uri)
        }
        pfd
    }

    private suspend fun <T : Model> export(
        uri: Uri,
        typeDescription: String,
        version: Int,
        progress: MutableLiveData<Pair<Int, Int>>,
        list: List<T>,
        writeJsonRecord: (record: T, writer: JsonWriter) -> Unit,
    ) = withContext(Dispatchers.IO) {
        openFile(uri)?.use {
            try {
                JsonWriter(OutputStreamWriter(FileOutputStream(it.fileDescriptor), "UTF-8")).use { writer ->
                    writer.setIndent("  ")
                    writer.beginObject()
                    writer.name(Constants.NAME_TYPE).value(typeDescription)
                    writer.name(Constants.NAME_VERSION).value(version)
                    writer.name(Constants.NAME_ITEMS)
                    writer.beginArray()

                    list.forEachWithIndex { index, record ->
                        progress.postValue(list.size to index)
                        try {
                            writeJsonRecord(record, writer)
                        } catch (e: RuntimeException) {
                            Timber.e(e)
                        }
                    }
                    progress.postValue(list.size to list.size)

                    writer.endArray()
                    writer.endObject()
                }

                postMessage(R.string.msg_export_success)
            } catch (e: Exception) {
                Timber.e(e)
                postMessage(R.string.msg_export_failed_write_json)
            }
        }
    }

    private fun postMessage(@StringRes resId: Int, vararg formatArgs: Any?) {
        _message.postValue(Event(getApplication<BggApplication>().getString(resId, *formatArgs)))
    }

    fun importCollectionViews(uri: Uri) {
        viewModelScope.launch {
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
    }

    fun importGames(uri: Uri) {
        viewModelScope.launch {
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
    }

    fun importUsers(uri: Uri) {
        viewModelScope.launch {
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
    }

    private fun openFileToRead(uri: Uri): ParcelFileDescriptor? {
        val pfd = try {
            getApplication<BggApplication>().contentResolver.openFileDescriptor(uri, "r")
        } catch (e: SecurityException) {
            Timber.w(e)
            postMessage(R.string.msg_import_failed_file_not_read, uri)
            return null
        } catch (e: FileNotFoundException) {
            Timber.w(e)
            postMessage(R.string.msg_import_failed_file_not_exist, uri)
            return null
        }
        if (pfd == null) {
            postMessage(R.string.msg_export_failed_null_pfd, uri)
        }
        return pfd
    }

    private suspend fun <T> import(
        uri: Uri,
        typeDescription: String,
        progress: MutableLiveData<Pair<Int, Int>>,
        parseItem: (reader: JsonReader) -> T,
        importRecord: (item: T, version: Int) -> Unit,
        initializeImport: () -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val items: MutableList<T> = mutableListOf()
        var version = 0
        openFileToRead(uri)?.use { pfd ->
            val reader = JsonReader(InputStreamReader(FileInputStream(pfd.fileDescriptor), "UTF-8"))
            try {
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        items += parseItem(reader)
                    }
                    reader.endArray()
                } else {
                    var shouldContinue = true
                    reader.beginObject()
                    while (reader.hasNext() && shouldContinue) {
                        when (reader.nextName()) {
                            Constants.NAME_TYPE -> reader.nextString().also {
                                if (it != typeDescription) {
                                    progress.postValue(items.size to items.size)
                                    postMessage(R.string.msg_import_failed_wrong_type, typeDescription, it!!)
                                    shouldContinue = false
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
        }

        progress.postValue(items.size to items.size)
        postMessage(R.string.msg_import_success)
    }
}
