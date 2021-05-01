package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.extensions.CollectionView.DEFAULT_DEFAULT_ID
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CollectionItemRepository
import com.boardgamegeek.repository.CollectionViewRepository
import com.boardgamegeek.sorter.CollectionSorterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CollectionViewViewModel(application: Application) : AndroidViewModel(application) {
    private val viewRepository = CollectionViewRepository(getApplication())
    private val itemRepository = CollectionItemRepository(getApplication())

    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val defaultViewId
        get() = prefs[CollectionView.PREFERENCES_KEY_DEFAULT_ID, DEFAULT_DEFAULT_ID]
                ?: DEFAULT_DEFAULT_ID

    private val collectionFiltererFactory: CollectionFiltererFactory by lazy { CollectionFiltererFactory(application) }
    private val collectionSorterFactory: CollectionSorterFactory by lazy { CollectionSorterFactory(application) }

    val views: LiveData<List<CollectionViewEntity>> = liveData { emit(viewRepository.load()) }

    private val syncTimestamp = MutableLiveData<Long>()
    private val _sortType = MutableLiveData<Int>()
    private val _addedFilters = MutableLiveData<List<CollectionFilterer>>()
    private val _removedFilterTypes = MutableLiveData<List<Int>>()

    private val _effectiveSortType = MediatorLiveData<Int>()
    val effectiveSortType: LiveData<Int>
        get() = _effectiveSortType

    private val _effectiveFilters = MediatorLiveData<List<CollectionFilterer>>()
    val effectiveFilters: LiveData<List<CollectionFilterer>>
        get() = _effectiveFilters

    private val _items = MediatorLiveData<List<CollectionItemEntity>>()
    val items: LiveData<List<CollectionItemEntity>>
        get() = _items

    private val _allItems: LiveData<RefreshableResource<List<CollectionItemEntity>>> = syncTimestamp.switchMap {
        itemRepository.loadCollection()
    }

    val isRefreshing: LiveData<Boolean> = _allItems.map {
        it.status == Status.REFRESHING
    }

    private val _selectedViewId = MutableLiveData<Long>()
    val selectedViewId: LiveData<Long>
        get() = _selectedViewId

    private val selectedView: LiveData<CollectionViewEntity> = _selectedViewId.switchMap {
        liveData {
            _sortType.value = CollectionSorterFactory.TYPE_UNKNOWN
            _addedFilters.value = emptyList()
            _removedFilterTypes.value = emptyList()
            emit(viewRepository.load(it))
        }
    }

    val selectedViewName: LiveData<String> = selectedView.map {
        it.name
    }

    init {
        viewModelScope.launch { initMediators() }
        _selectedViewId.value = defaultViewId
    }

    private suspend fun initMediators() = withContext(Dispatchers.Default) {
        _effectiveSortType.addSource(selectedView) {
            createEffectiveSort(it, _sortType.value)
        }
        _effectiveSortType.addSource(_sortType) {
            createEffectiveSort(selectedView.value, it)
        }

        _effectiveFilters.addSource(selectedView) {
            createEffectiveFilters(it,
                    _addedFilters.value.orEmpty(),
                    _removedFilterTypes.value.orEmpty()
            )
        }
        _effectiveFilters.addSource(_addedFilters) {
            createEffectiveFilters(selectedView.value,
                    it,
                    _removedFilterTypes.value.orEmpty()
            )
        }
        _effectiveFilters.addSource(_removedFilterTypes) {
            createEffectiveFilters(selectedView.value,
                    _addedFilters.value.orEmpty(),
                    it
            )
        }

        _items.addSource(effectiveFilters) {
            filterAndSortItems(filters = it)
        }
        _items.addSource(effectiveSortType) {
            filterAndSortItems(sortType = it)
        }
        _items.addSource(_allItems) {
            filterAndSortItems(itemList = it.data.orEmpty())
        }
    }

    fun selectView(viewId: Long) {
        if (_selectedViewId.value != viewId) {
            viewModelScope.launch { viewRepository.updateShortcuts(viewId) }
            _selectedViewId.value = viewId
        }
    }

    fun setSort(sortType: Int) {
        val type = when (sortType) {
            CollectionSorterFactory.TYPE_UNKNOWN -> CollectionSorterFactory.TYPE_DEFAULT
            else -> sortType
        }
        if (_sortType.value != sortType) _sortType.value = type
    }

    fun addFilter(filter: CollectionFilterer) {
        viewModelScope.launch(Dispatchers.Default) {
            if (filter.isValid) {
                val removedFilters = _removedFilterTypes.value?.toMutableList() ?: mutableListOf()
                if (removedFilters.remove(filter.type)) {
                    _removedFilterTypes.value = removedFilters
                }

                val filters = _addedFilters.value?.toMutableList() ?: mutableListOf()
                filters.apply {
                    remove(filter)
                    add(filter)
                }
                _addedFilters.postValue(filters)
            }
        }
    }

    fun removeFilter(type: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val filters = _addedFilters.value?.toMutableList() ?: mutableListOf()
            filters.find { it.type == type }?.let {
                if (filters.remove(it)) {
                    _addedFilters.value = filters
                }
            }

            val removedFilters = _removedFilterTypes.value?.toMutableList() ?: mutableListOf()
            removedFilters.apply {
                remove(type)
                add(type)
            }
            _removedFilterTypes.postValue(removedFilters)
        }
    }

    private fun createEffectiveFilters(loadedView: CollectionViewEntity?, addedFilters: List<CollectionFilterer>, removedFilterTypes: List<Int>) {
        viewModelScope.launch(Dispatchers.Default) {
            // inflate filters
            val loadedFilters = mutableListOf<CollectionFilterer>()
            for ((type, data) in loadedView?.filters.orEmpty()) {
                val filter = collectionFiltererFactory.create(type)
                filter?.inflate(data)
                if (filter?.isValid == true) {
                    loadedFilters.add(filter)
                }
            }

            val addedTypes = addedFilters.map { af -> af.type }
            val filters: MutableList<CollectionFilterer> = mutableListOf()
            loadedFilters.forEach { lf ->
                if (!addedTypes.contains(lf.type) && !removedFilterTypes.contains(lf.type))
                    filters.add(lf)
            }
            addedFilters.forEach { af ->
                if (!removedFilterTypes.contains(af.type))
                    filters.add(af)
            }
            _effectiveFilters.postValue(filters)
        }
    }

    private fun createEffectiveSort(loadedView: CollectionViewEntity?, sortType: Int?) {
        _effectiveSortType.value = if (sortType == null || sortType == CollectionSorterFactory.TYPE_UNKNOWN) {
            loadedView?.sortType ?: CollectionSorterFactory.TYPE_DEFAULT
        } else {
            sortType
        }
    }

    private fun filterAndSortItems(
            itemList: List<CollectionItemEntity>? = _allItems.value?.data,
            filters: List<CollectionFilterer> = effectiveFilters.value.orEmpty(),
            sortType: Int = effectiveSortType.value ?: CollectionSorterFactory.TYPE_DEFAULT,
    ) {
        if (itemList == null) return
        viewModelScope.launch(Dispatchers.Default) {
            var list = itemList.asSequence()
            if (_selectedViewId.value == DEFAULT_DEFAULT_ID) {
                list = list.filter {
                    (prefs.isStatusSetToSync(COLLECTION_STATUS_OWN) && it.own) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_PREVIOUSLY_OWNED) && it.previouslyOwned) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_FOR_TRADE) && it.forTrade) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WANT_IN_TRADE) && it.wantInTrade) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WANT_TO_BUY) && it.wantToPlay) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WISHLIST) && it.wishList) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WANT_TO_PLAY) && it.wantToPlay) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_PREORDERED) && it.preOrdered) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_PLAYED) && it.numberOfPlays > 1) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_RATED) && it.rating > 0.0) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_COMMENTED) && it.comment.isNotBlank()) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_HAS_PARTS) && it.hasPartsList.isNotBlank()) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WANT_PARTS) && it.wantPartsList.isNotBlank())
                }.asSequence()
            } else {
                filters.forEach { f ->
                    list = list.filter { f.filter(it) }.asSequence()
                }
            }
            val sorter = collectionSorterFactory.create(sortType)
            _items.postValue(sorter?.sort(list.toList()) ?: list.toList())
        }
    }

    fun refresh(): Boolean {
        return if ((syncTimestamp.value ?: 0).isOlderThan(1, TimeUnit.MINUTES)) {
            syncTimestamp.postValue(System.currentTimeMillis())
            true
        } else false
    }

    fun insert(name: String, isDefault: Boolean) {
        viewModelScope.launch {
            val view = CollectionViewEntity(
                    id = 0L, //ignored
                    name = name,
                    sortType = effectiveSortType.value ?: CollectionSorterFactory.TYPE_DEFAULT,
                    filters = effectiveFilters.value?.map { CollectionViewFilterEntity(it.type, it.deflate()) },
            )
            val viewId = viewRepository.insertView(view)
            setOrRemoveDefault(viewId, isDefault)
            selectView(viewId)
        }
    }

    fun update(isDefault: Boolean) {
        viewModelScope.launch {
            val view = CollectionViewEntity(
                    id = _selectedViewId.value ?: BggContract.INVALID_ID.toLong(),
                    name = selectedViewName.value.orEmpty(),
                    sortType = effectiveSortType.value ?: CollectionSorterFactory.TYPE_DEFAULT,
                    filters = effectiveFilters.value?.map { CollectionViewFilterEntity(it.type, it.deflate()) },
            )
            viewRepository.updateView(view)
            setOrRemoveDefault(view.id, isDefault)
        }
    }

    fun deleteView(viewId: Long) {
        viewModelScope.launch {
            viewRepository.deleteView(viewId)
            if (viewId == _selectedViewId.value) {
                selectView(defaultViewId)
            }
        }
    }

    private fun setOrRemoveDefault(viewId: Long, isDefault: Boolean) {
        if (isDefault) {
            prefs[CollectionView.PREFERENCES_KEY_DEFAULT_ID] = viewId
        } else {
            if (viewId == defaultViewId) {
                prefs.remove(CollectionView.PREFERENCES_KEY_DEFAULT_ID)
            }
        }
    }
}
