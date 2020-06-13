package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.tasks.*
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask
import com.boardgamegeek.ui.model.PrivateInfo

class GameCollectionItemViewModel(application: Application) : AndroidViewModel(application) {
    private val gameCollectionRepository = GameCollectionRepository(getApplication())

    private val _collectionId = MutableLiveData<Int>()
    val collectionId: LiveData<Int>
        get() = _collectionId

    val isEdited = MutableLiveData<Boolean>()

    init {
        isEdited.value = false
    }

    fun setId(id: Int?) {
        if (_collectionId.value != id) _collectionId.value = id
    }

    val item: LiveData<RefreshableResource<CollectionItemEntity>> = Transformations.switchMap(_collectionId) { id ->
        when (id) {
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> gameCollectionRepository.getCollectionItem(id)
        }
    }

    fun refresh() {
        // TODO test if we're already refreshing?
        _collectionId.value?.let { _collectionId.value = it }
    }

    fun update(privateInfo: PrivateInfo) {
        setEdited(true)
        val gameId = item.value?.data?.gameId ?: BggContract.INVALID_ID
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        UpdateCollectionItemPrivateInfoTask(getApplication(),
                gameId,
                collectionId.value ?: BggContract.INVALID_ID,
                internalId,
                privateInfo).executeAsyncTask()
    }

    fun update(text: String, textColumn: String, timestampColumn: String) {
        setEdited(true)
        val gameId = item.value?.data?.gameId ?: BggContract.INVALID_ID
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        UpdateCollectionItemTextTask(getApplication(),
                gameId,
                collectionId.value ?: BggContract.INVALID_ID,
                internalId,
                text, textColumn, timestampColumn).executeAsyncTask()
    }

    fun update(statuses: List<String>, wishlistPriority: Int) {
        val gameId = item.value?.data?.gameId ?: BggContract.INVALID_ID
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        UpdateCollectionItemStatusTask(getApplication(),
                gameId,
                collectionId.value ?: BggContract.INVALID_ID,
                internalId,
                statuses,
                wishlistPriority).executeAsyncTask()
    }

    fun delete() {
        setEdited(false)
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        DeleteCollectionItemTask(getApplication(), internalId).executeAsyncTask()
    }

    fun reset() {
        setEdited(false)
        val gameId = item.value?.data?.gameId ?: BggContract.INVALID_ID
        val internalId = item.value?.data?.internalId ?: BggContract.INVALID_ID.toLong()
        ResetCollectionItemTask(getApplication(), internalId).executeAsyncTask()
        // TODO wait until reset is done
        SyncCollectionByGameTask(getApplication() as BggApplication, gameId).executeAsyncTask()
    }

    private fun setEdited(edited: Boolean) {
        if (isEdited.value != edited) isEdited.value = edited
    }

}