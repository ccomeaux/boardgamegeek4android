package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.R
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.extensions.CollectionView.DEFAULT_DEFAULT_ID
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CollectionItemRepository
import com.boardgamegeek.repository.CollectionViewRepository
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.tasks.SelectCollectionViewTask
import java.util.concurrent.TimeUnit

class CollectionViewViewModel(application: Application) : AndroidViewModel(application) {
    private val viewRepository = CollectionViewRepository(getApplication())
    private val itemRepository = CollectionItemRepository(getApplication())

    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val defaultViewId
        get() = prefs[CollectionView.PREFERENCES_KEY_DEFAULT_ID, DEFAULT_DEFAULT_ID] ?: DEFAULT_DEFAULT_ID

    private val collectionFiltererFactory: CollectionFiltererFactory by lazy { CollectionFiltererFactory(application) }
    private val collectionSorterFactory: CollectionSorterFactory by lazy { CollectionSorterFactory(application) }

    val views: LiveData<List<CollectionViewEntity>> = viewRepository.load()

    private val _selectedViewId = MutableLiveData<Long>()
    private val _sortType = MutableLiveData<Int>()
    private val _addedFilters = MutableLiveData<List<CollectionFilterer>>()
    private val _removedFilters = MutableLiveData<List<Int>>()

    val effectiveSortType = MediatorLiveData<Int>()
    val effectiveFilters = MediatorLiveData<List<CollectionFilterer>>()
    val items = MediatorLiveData<List<CollectionItemEntity>>()
    private val syncTimestamp = MutableLiveData<Long>()
    private val _items: LiveData<RefreshableResource<List<CollectionItemEntity>>> = Transformations.switchMap(syncTimestamp){
        itemRepository.loadCollection()
    }
    val isRefreshing: LiveData<Boolean> = Transformations.map(_items) {
        it.status == Status.REFRESHING
    }

    private val selectedView: LiveData<CollectionViewEntity> = Transformations.switchMap(_selectedViewId) {
        _sortType.value = CollectionSorterFactory.TYPE_UNKNOWN
        _addedFilters.value = emptyList()
        _removedFilters.value = emptyList()
        viewRepository.load(it)
    }

    init {
        effectiveSortType.addSource(selectedView) {
            createEffectiveSort(it, _sortType.value)
        }
        effectiveSortType.addSource(_sortType) {
            createEffectiveSort(selectedView.value, it)
        }

        effectiveFilters.addSource(selectedView) {
            createEffectiveFilters(it,
                    _addedFilters.value.orEmpty(),
                    _removedFilters.value.orEmpty()
            )
        }
        effectiveFilters.addSource(_addedFilters) {
            createEffectiveFilters(selectedView.value,
                    it,
                    _removedFilters.value.orEmpty()
            )
        }
        effectiveFilters.addSource(_removedFilters) {
            createEffectiveFilters(selectedView.value,
                    _addedFilters.value.orEmpty(),
                    it
            )
        }

        items.addSource(effectiveFilters) {
            filterAndSortItems(filters = it)
        }
        items.addSource(effectiveSortType) {
            filterAndSortItems(sortType = it)
        }
        items.addSource(_items) {
            filterAndSortItems(itemList = it.data.orEmpty())
        }

        _selectedViewId.value = defaultViewId
    }

    val selectedViewId: LiveData<Long>
        get() = _selectedViewId

    private fun createEffectiveFilters(loadedView: CollectionViewEntity?, addedFilters: List<CollectionFilterer>, removedFilters: List<Int>) {
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
            if (!addedTypes.contains(lf.type) && !removedFilters.contains(lf.type))
                filters.add(lf)
        }
        addedFilters.forEach { af ->
            if (!removedFilters.contains(af.type))
                filters.add(af)
        }
        effectiveFilters.value = filters
    }

    private fun createEffectiveSort(loadedView: CollectionViewEntity?, sortType: Int?) {
        effectiveSortType.value = if (sortType == null || sortType == CollectionSorterFactory.TYPE_UNKNOWN) {
            loadedView?.sortType ?: CollectionSorterFactory.TYPE_DEFAULT
        } else {
            sortType
        }
    }

    fun selectView(viewId: Long) {
        if (_selectedViewId.value != viewId) {
            SelectCollectionViewTask(getApplication(), viewId).execute()
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
        if (filter.isValid) {
            val removedFilters = _removedFilters.value?.toMutableList() ?: mutableListOf()
            if (removedFilters.remove(filter.type)) {
                _removedFilters.value = removedFilters
            }

            val filters = _addedFilters.value?.toMutableList() ?: mutableListOf()
            filters.apply {
                remove(filter)
                add(filter)
            }
            _addedFilters.value = filters
        }
    }

    fun removeFilter(type: Int) {
        val filters = _addedFilters.value?.toMutableList() ?: mutableListOf()
        filters.find { it.type == type }?.let {
            filters.remove(it)
            _addedFilters.value = filters
        }

        val removedFilters = _removedFilters.value?.toMutableList() ?: mutableListOf()
        removedFilters.apply {
            remove(type)
            add(type)
        }
        _removedFilters.value = removedFilters
    }

    val selectedViewName: LiveData<String> = Transformations.map(selectedView) {
        it?.name ?: application.getString(R.string.title_collection)
    }

    private fun filterAndSortItems(
            itemList: List<CollectionItemEntity>? = _items.value?.data,
            filters: List<CollectionFilterer> = effectiveFilters.value.orEmpty(),
            sortType: Int = effectiveSortType.value ?: CollectionSorterFactory.TYPE_DEFAULT,
    ) {
        if (itemList == null) return
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
        if (sorter == null) {
            items.value = list.toList()
        } else {
            items.value = sorter.sort(list.toList())
        }
    }

    fun refresh(): Boolean {
        return if ((syncTimestamp.value ?: 0).isOlderThan(1, TimeUnit.MINUTES)) {
            syncTimestamp.postValue(System.currentTimeMillis())
            true
        } else false
    }

    fun insert(name: String, isDefault: Boolean): Long {
        val filterEntities = mutableListOf<CollectionViewFilterEntity>()
        effectiveFilters.value?.forEach { f ->
            filterEntities.add(CollectionViewFilterEntity(f.type, f.deflate()))
        }
        val view = CollectionViewEntity(
                0L, //ignored
                name,
                _sortType.value ?: CollectionSorterFactory.TYPE_DEFAULT,
                filterEntities
        )
        val viewId = viewRepository.insertView(view)
        setOrRemoveDefault(viewId, isDefault)
        selectView(viewId)
        return viewId
    }

    fun update(isDefault: Boolean) {
        val filterEntities = mutableListOf<CollectionViewFilterEntity>()
        effectiveFilters.value?.forEach { f ->
            filterEntities.add(CollectionViewFilterEntity(f.type, f.deflate()))
        }
        val view = CollectionViewEntity(
                _selectedViewId.value ?: BggContract.INVALID_ID.toLong(),
                selectedViewName.value ?: "",
                _sortType.value ?: CollectionSorterFactory.TYPE_DEFAULT,
                filterEntities
        )
        viewRepository.updateView(view)
        setOrRemoveDefault(view.id, isDefault)
    }

    fun deleteView(viewId: Long) {
        viewRepository.deleteView(viewId)
        if (viewId == _selectedViewId.value) {
            selectView(defaultViewId)
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
