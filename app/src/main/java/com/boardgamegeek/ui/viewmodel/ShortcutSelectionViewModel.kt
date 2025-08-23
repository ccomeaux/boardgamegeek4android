package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.lifecycle.*
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.ensureHttpsScheme
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.ImageRepository
import com.boardgamegeek.ui.GameActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShortcutSelectionViewModel @Inject constructor(
    application: Application,
    private val gameCollectionRepository: GameCollectionRepository,
    private val imageRepository: ImageRepository,
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

    private val _createdShortcut = MutableLiveData<Event<ShortcutInfoCompat>>()
    val createdShortcut: LiveData<Event<ShortcutInfoCompat>>
        get() = _createdShortcut

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

    fun createShortcut(item: CollectionItem) {
        viewModelScope.launch {
            val bitmap = imageRepository.fetchThumbnail(item.robustThumbnailUrl.ensureHttpsScheme())
            val shortcutInfoCompat = GameActivity.createShortcutInfo(getApplication(), item.gameId, item.gameName, bitmap)
            if (shortcutInfoCompat == null) {
                _errorMessage.postValue(Event(getApplication<BggApplication>().resources.getString(R.string.error_shortcut_create)))
            } else {
                _createdShortcut.postValue(Event(shortcutInfoCompat))
            }
        }
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
