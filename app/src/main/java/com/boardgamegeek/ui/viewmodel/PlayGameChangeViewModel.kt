package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.repository.GameCollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@HiltViewModel
class PlayGameChangeViewModel @Inject constructor(
    application: Application,
    private val gameCollectionRepository: GameCollectionRepository,
) : AndroidViewModel(application) {
    private val _items = MediatorLiveData<List<CollectionItem>?>()
    val items: LiveData<List<CollectionItem>?>
        get() = _items

    private val _filter = MutableLiveData<String>()
    val filter: LiveData<String>
        get() = _filter

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _allItems: LiveData<List<CollectionItem>> =
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                emitSource(gameCollectionRepository.loadAllAsFlow().distinctUntilChanged().asLiveData())
            } catch (e: Exception) {
                _errorMessage.postValue(Event(e.localizedMessage.ifEmpty { "Error loading collection" }))
            }
        }

    init {
        _items.addSource(_allItems) { result ->
            result?.let {
                _items.value = filterItems(allItems = result)
            }
        }
        _items.addSource(_filter) { result ->
            result?.let {
                _items.value = filterItems(filter = result)
            }
        }
        filter("")
    }

    fun filter(filter: String) {
        if (_filter.value != filter) _filter.value = filter
    }

    private fun filterItems(
        allItems: List<CollectionItem>? = _allItems.value,
        filter: String? = _filter.value,
    ): List<CollectionItem>? {
        return filter?.let { filterText ->
            allItems?.filter {
                it.collectionName.contains(filterText, true)
            }
        }
    }
}
