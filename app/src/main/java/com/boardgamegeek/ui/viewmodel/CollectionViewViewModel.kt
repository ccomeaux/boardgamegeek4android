package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.entities.CollectionViewFilterEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CollectionViewRepository
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.tasks.SelectCollectionViewTask

class CollectionViewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CollectionViewRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }
    private val defaultViewId
        get() = prefs[CollectionView.PREFERENCES_KEY_DEFAULT_ID, CollectionView.DEFAULT_DEFAULT_ID]
                ?: CollectionView.DEFAULT_DEFAULT_ID

    val views: LiveData<List<CollectionViewEntity>> = repository.load()

    private val _selectedViewId = MutableLiveData<Long>()
    private val _sortType = MutableLiveData<Int>()
    private val _addedFilters = MutableLiveData<MutableList<CollectionFilterer>>()
    private val _removedFilters = MutableLiveData<MutableList<Int>>()

    val effectiveSortType = MediatorLiveData<Int>()
    val effectiveFilters = MediatorLiveData<MutableList<CollectionFilterer>>()

    private val selectedView: LiveData<CollectionViewEntity> = Transformations.switchMap(_selectedViewId) {
        _sortType.value = CollectionSorterFactory.TYPE_UNKNOWN
        _addedFilters.value?.clear()
        _removedFilters.value?.clear()
        repository.load(it)
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
                    _addedFilters.value ?: emptyList(),
                    _removedFilters.value ?: emptyList()
            )
        }
        effectiveFilters.addSource(_addedFilters) {
            createEffectiveFilters(selectedView.value,
                    it,
                    _removedFilters.value ?: emptyList()
            )
        }
        effectiveFilters.addSource(_removedFilters) {
            createEffectiveFilters(selectedView.value,
                    _addedFilters.value ?: emptyList(),
                    it
            )
        }
        _selectedViewId.value = defaultViewId
    }

    val selectedViewId: LiveData<Long>
        get() = _selectedViewId

    private fun createEffectiveFilters(loadedView: CollectionViewEntity?, addedFilters: List<CollectionFilterer>, removedFilters: List<Int>) {
        // inflate filters
        val collectionFiltererFactory = CollectionFiltererFactory(getApplication())
        val loadedFilters = mutableListOf<CollectionFilterer>()
        for ((type, data) in loadedView?.filters ?: emptyList()) {
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
            val filters = _addedFilters.value ?: mutableListOf()
            filters.apply {
                remove(filter)
                add(filter)
            }
            _addedFilters.value = filters
        }
    }

    fun removeFilter(type: Int) {
        val filters = _addedFilters.value ?: mutableListOf()
        for (filter in filters) {
            if (filter.type == type) {
                filters.remove(filter)
                _addedFilters.value = filters
                break
            }
        }

        val removedFilters = _removedFilters.value ?: mutableListOf()
        removedFilters.apply {
            remove(type)
            add(type)
        }
        _removedFilters.value = removedFilters
    }

    val selectedViewName: LiveData<String> = Transformations.map(selectedView) {
        it?.name ?: application.getString(R.string.title_collection)
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
        val viewId = repository.insertView(view)
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
        repository.updateView(view)
        setOrRemoveDefault(view.id, isDefault)
    }

    fun deleteView(viewId: Long) {
        repository.deleteView(viewId)
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
