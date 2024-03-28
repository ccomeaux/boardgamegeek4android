package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.core.os.bundleOf
import androidx.lifecycle.*
import androidx.work.WorkManager
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.model.PlayUploadResult
import com.boardgamegeek.extensions.*
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.filterer.CollectionFiltererFactory
import com.boardgamegeek.livedata.Event
import com.boardgamegeek.livedata.ThrottledLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.CollectionViewRepository
import com.boardgamegeek.repository.GameCollectionRepository
import com.boardgamegeek.repository.PlayRepository
import com.boardgamegeek.sorter.CollectionSorter
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CollectionViewViewModel @Inject constructor(
    application: Application,
    private val viewRepository: CollectionViewRepository,
    private val playRepository: PlayRepository,
    private val gameCollectionRepository: GameCollectionRepository,
) : AndroidViewModel(application) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(getApplication())

    private val prefs: SharedPreferences by lazy { application.preferences() }
    val defaultViewId
        get() = prefs[CollectionViewPrefs.PREFERENCES_KEY_DEFAULT_ID, CollectionViewPrefs.DEFAULT_DEFAULT_ID] ?: CollectionViewPrefs.DEFAULT_DEFAULT_ID

    private val collectionSorterFactory: CollectionSorterFactory by lazy { CollectionSorterFactory(application) }

    private val _sortType = MutableLiveData<Int>()
    private val _addedFilters = MutableLiveData<List<CollectionFilterer>>()
    private val _removedFilterTypes = MutableLiveData<List<Int>>()

    private val _effectiveSort = MediatorLiveData<Pair<CollectionSorter, Boolean>>()
    val effectiveSort: LiveData<Pair<CollectionSorter, Boolean>>
        get() = _effectiveSort

    private val _effectiveFilters = MediatorLiveData<List<CollectionFilterer>>()
    val effectiveFilters: LiveData<List<CollectionFilterer>>
        get() = _effectiveFilters

    private val _items = MediatorLiveData<List<CollectionItem>>()
    val items: LiveData<List<CollectionItem>>
        get() = ThrottledLiveData(_items, 500)

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _toastMessage = MediatorLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>>
        get() = _toastMessage

    private val _loggedPlayResult = MutableLiveData<Event<PlayUploadResult>>()
    val loggedPlayResult: LiveData<Event<PlayUploadResult>>
        get() = _loggedPlayResult

    private val _isFiltering = MediatorLiveData<Boolean>()
    val isFiltering: LiveData<Boolean>
        get() = _isFiltering

    val isRefreshing = WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkLiveData(workName).map { list ->
        list.any { workInfo -> !workInfo.state.isFinished }
    }

    private val _allItems: LiveData<List<CollectionItem>> =
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                emitSource(gameCollectionRepository.loadAllAsFlow().distinctUntilChanged().asLiveData())
            } catch (e: Exception) {
                _errorMessage.postValue(Event(e.localizedMessage.ifEmpty { "Error loading collection" }))
            }
        }

    private val _selectedViewId = MutableLiveData<Int>()
    val selectedViewId: LiveData<Int>
        get() = _selectedViewId

    val views: LiveData<List<CollectionView>> =
        liveData {
            emitSource(viewRepository.loadViewsWithoutFiltersFlow().distinctUntilChanged().asLiveData())
        }

    private val selectedView: LiveData<CollectionView> = _selectedViewId.switchMap {
        liveData {
            _sortType.postValue(CollectionSorterFactory.TYPE_UNKNOWN)
            _addedFilters.postValue(emptyList())
            _removedFilterTypes.postValue(emptyList())
            if (it == CollectionViewPrefs.DEFAULT_DEFAULT_ID)
                emit(viewRepository.defaultView)
            else
                emitSource(viewRepository.loadViewFlow(it).distinctUntilChanged().asLiveData())
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
        _effectiveSort.addSource(selectedView) {
            createEffectiveSort(it, _sortType.value)
        }
        _effectiveSort.addSource(_sortType) {
            createEffectiveSort(selectedView.value, it)
        }

        _effectiveFilters.addSource(selectedView) {
            createEffectiveFilters(
                it,
                _addedFilters.value.orEmpty(),
                _removedFilterTypes.value.orEmpty()
            )
        }
        _effectiveFilters.addSource(_addedFilters) {
            createEffectiveFilters(
                selectedView.value,
                it,
                _removedFilterTypes.value.orEmpty()
            )
        }
        _effectiveFilters.addSource(_removedFilterTypes) {
            createEffectiveFilters(
                selectedView.value,
                _addedFilters.value.orEmpty(),
                it
            )
        }

        _items.addSource(effectiveFilters) {
            filterAndSortItems(filters = it)
        }
        _items.addSource(effectiveSort) {
            filterAndSortItems(sortType = it)
        }
        _items.addSource(_allItems) {
            it?.let { filterAndSortItems(itemList = it) }
        }
    }

    fun selectView(viewId: Int) {
        if (_selectedViewId.value != viewId) {
            _isFiltering.postValue(true)
            viewModelScope.launch { viewRepository.updateShortcuts(viewId) }
            _selectedViewId.value = viewId
        }
    }

    fun findViewId(viewName: String) = views.value?.find { it.name == viewName }?.id ?: BggContract.INVALID_ID

    fun setSort(sortType: Int) {
        val type = when (sortType) {
            CollectionSorterFactory.TYPE_UNKNOWN -> CollectionSorterFactory.TYPE_DEFAULT
            else -> sortType
        }
        if (_sortType.value != type) {
            _isFiltering.postValue(true)
            _sortType.value = type
        }
    }

    fun reverseSort() {
        _sortType.value?.let { type ->
            collectionSorterFactory.reverse(type)?.let { reversedSortType ->
                setSort(reversedSortType)
            }
        }
    }

    fun addFilter(filter: CollectionFilterer) {
        viewModelScope.launch(Dispatchers.Default) {
            _isFiltering.postValue(true)
            if (filter.isValid) {
                val removedFilters = _removedFilterTypes.value.orEmpty().toMutableList()
                if (removedFilters.remove(filter.type)) {
                    _removedFilterTypes.postValue(removedFilters)
                }

                val filters = _addedFilters.value.orEmpty().toMutableList()
                filters.apply {
                    remove(filter)
                    add(filter)
                }
                firebaseAnalytics.logEvent(
                    "Filter",
                    bundleOf(
                        FirebaseAnalytics.Param.CONTENT_TYPE to "Collection",
                        "FilterBy" to filter.type.toString()
                    )
                )
                _addedFilters.postValue(filters)
            }
        }
    }

    fun removeFilter(type: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            _isFiltering.postValue(true)

            val filters = _addedFilters.value.orEmpty().toMutableList()
            filters.find { it.type == type }?.let {
                if (filters.remove(it)) {
                    _addedFilters.postValue(filters)
                }
            }

            val removedFilters = _removedFilterTypes.value.orEmpty().toMutableList()
            removedFilters.apply {
                remove(type)
                add(type)
            }
            _removedFilterTypes.postValue(removedFilters)
        }
    }

    val acquiredFrom = liveData {
        emit(gameCollectionRepository.loadAcquiredFrom())
    }

    val inventoryLocation = liveData {
        emit(gameCollectionRepository.loadInventoryLocation())
    }

    private fun createEffectiveFilters(
        loadedView: CollectionView?,
        addedFilters: List<CollectionFilterer>,
        removedFilterTypes: List<Int>
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val addedTypes = addedFilters.map { it.type }
            val filters = loadedView?.filters.orEmpty().filter { !addedTypes.contains(it.type) && !removedFilterTypes.contains(it.type) } +
                    addedFilters.filter { !removedFilterTypes.contains(it.type) }
            _effectiveFilters.postValue(filters)
        }
    }

    private fun createEffectiveSort(loadedView: CollectionView?, sortType: Int?) {
        val type = if (sortType == null || sortType == CollectionSorterFactory.TYPE_UNKNOWN) {
            loadedView?.sortType ?: CollectionSorterFactory.TYPE_DEFAULT
        } else {
            sortType
        }
        _effectiveSort.postValue(collectionSorterFactory.create(type))
    }

    private fun filterAndSortItems(
        itemList: List<CollectionItem>? = _allItems.value,
        filters: List<CollectionFilterer> = effectiveFilters.value.orEmpty(),
        sortType: Pair<CollectionSorter, Boolean>? = effectiveSort.value,
    ) {
        if (itemList == null) return
        viewModelScope.launch(Dispatchers.Default) {
            var list = itemList.asSequence()
            if (_selectedViewId.value == CollectionViewPrefs.DEFAULT_DEFAULT_ID && filters.none { it.type == CollectionFiltererFactory.TYPE_STATUS }) {
                list = list.filter {
                    (prefs.isStatusSetToSync(COLLECTION_STATUS_OWN) && it.own) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_PREVIOUSLY_OWNED) && it.previouslyOwned) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_FOR_TRADE) && it.forTrade) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WANT_IN_TRADE) && it.wantInTrade) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WANT_TO_BUY) && it.wantToPlay) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WISHLIST) && it.wishList) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WANT_TO_PLAY) && it.wantToPlay) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_PREORDERED) && it.preOrdered) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_PLAYED) && it.numberOfPlays > 0) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_RATED) && it.rating > 0.0) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_COMMENTED) && it.comment.isNotBlank()) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_HAS_PARTS) && it.hasPartsList.isNotBlank()) ||
                            (prefs.isStatusSetToSync(COLLECTION_STATUS_WANT_PARTS) && it.wantPartsList.isNotBlank())
                }
            }
            filters.forEach { f ->
                list = list.filter { f.filter(it) }
            }
            sortType?.let {
                _items.postValue(it.first.sort(list.toList(), it.second))
            } ?: _items.postValue(list.toList())
        }
    }

    fun refresh() {
        if (isRefreshing.value == false) {
            gameCollectionRepository.enqueueRefreshRequest(workName)
        }
    }

    fun insert(name: String, isDefault: Boolean) {
        viewModelScope.launch {
            val view = constructView(0, name, isDefault)
            val viewId = viewRepository.insertView(view)
            logAction("Insert", name)
            postToastMessage(R.string.msg_collection_view_updated, name)
            selectView(viewId)
        }
    }

    fun update(name: String, isDefault: Boolean) {
        viewModelScope.launch {
            _selectedViewId.value?.let { viewId ->
                if (viewId != BggContract.INVALID_ID) {
                    val view = constructView(viewId, name, isDefault)
                    viewRepository.updateView(view)
                    logAction("Update", name)
                    postToastMessage(R.string.msg_collection_view_updated, name)
                }
            }
        }
    }

    private fun constructView(viewId: Int, name: String, isDefault: Boolean): CollectionView {
        return CollectionView(
            id = viewId,
            name = name,
            sortType = effectiveSort.value?.let { it.first.getType(it.second) } ?: CollectionSorterFactory.TYPE_DEFAULT,
            starred = isDefault,
            filters = effectiveFilters.value,
        )
    }

    fun deleteView(viewId: Int, name: String) {
        if (viewId <= 0) return
        viewModelScope.launch {
            if (viewRepository.deleteView(viewId)) {
                logAction("Delete", name)
                postToastMessage(R.string.msg_collection_view_deleted, name)
                if (viewId == _selectedViewId.value) {
                    selectView(defaultViewId)
                }
            }
        }
    }

    private fun logAction(action: String, name: String) {
        firebaseAnalytics.logEvent("DataManipulation") {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
            param("Action", action)
            param("Name", name)
        }
    }

    fun logQuickPlay(gameId: Int, gameName: String) {
        viewModelScope.launch {
            val result = playRepository.logQuickPlay(gameId, gameName)
            if (result.isFailure)
                postError(result.exceptionOrNull())
            else {
                result.getOrNull()?.let {
                    if (it.play.playId != BggContract.INVALID_ID)
                        _loggedPlayResult.value = Event(it)
                }
            }
        }
    }

    private fun postError(t: Throwable?) {
        _errorMessage.postValue(Event(t?.localizedMessage?.ifEmpty { "Unknown error" } ?: "Unknown error"))
    }

    private fun postToastMessage(resId: Int, name: String) {
        _toastMessage.value = Event(getApplication<BggApplication>().getString(resId, name))
    }

    fun createShortcut() {
        _selectedViewId.value?.let { viewId ->
            viewModelScope.launch {
                viewRepository.createViewShortcut(getApplication<BggApplication>().applicationContext, viewId, selectedViewName.value.orEmpty())
            }
        }
    }

    companion object {
        const val workName = "CollectionViewViewModel"
    }
}
