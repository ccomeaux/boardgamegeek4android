package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.PublisherRepository
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PublishersViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    private val publisherRepository = PublisherRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }

    private val _sort = MutableLiveData<PublishersSort>()
    val sort: LiveData<PublishersSort>
        get() = _sort

    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>>
        get() = _progress

    private var isCalculating = false

    init {
        val initialSort = if (prefs.isStatusSetToSync(COLLECTION_STATUS_RATED))
            SortType.WHITMORE_SCORE
        else
            SortType.ITEM_COUNT
        sort(initialSort)
    }

    val publishers = _sort.switchMap {
        liveData {
            val publishers = publisherRepository.loadPublishers(it.sortBy)
            emit(publishers)
            val lastCalculation = prefs[PREFERENCES_KEY_STATS_CALCULATED_TIMESTAMP_PUBLISHERS, 0L] ?: 0L
            if (!isCalculating && lastCalculation.isOlderThan(1, TimeUnit.HOURS)) {
                isCalculating = true
                val job = viewModelScope.launch { publisherRepository.calculateWhitmoreScores(publishers, _progress) }
                job.invokeOnCompletion {
                    refresh()
                    isCalculating = false
                }
            }
        }
    }

    fun sort(sortType: SortType) {
        if (_sort.value?.sortType != sortType) {
            _sort.value = when (sortType) {
                SortType.NAME -> PublishersSort.ByName()
                SortType.ITEM_COUNT -> PublishersSort.ByItemCount()
                SortType.WHITMORE_SCORE -> PublishersSort.ByWhitmoreScore()
            }
        }
    }

    fun refresh() {
        _sort.value?.let { _sort.value = it }
    }

    fun getSectionHeader(publisher: CompanyEntity?): String {
        return _sort.value?.getSectionHeader(publisher) ?: ""
    }

    sealed class PublishersSort {
        abstract val sortType: SortType
        abstract val sortBy: PublisherDao.SortType
        abstract fun getSectionHeader(publisher: CompanyEntity?): String

        class ByName : PublishersSort() {
            override val sortType = SortType.NAME
            override val sortBy = PublisherDao.SortType.NAME
            override fun getSectionHeader(publisher: CompanyEntity?): String {
                return when {
                    publisher == null -> ""
                    publisher.name.startsWith("(") -> "-"
                    else -> publisher.name.firstChar()
                }
            }
        }

        class ByItemCount : PublishersSort() {
            override val sortType = SortType.ITEM_COUNT
            override val sortBy = PublisherDao.SortType.ITEM_COUNT
            override fun getSectionHeader(publisher: CompanyEntity?): String {
                return (publisher?.itemCount ?: 0).orderOfMagnitude()
            }
        }

        class ByWhitmoreScore : PublishersSort() {
            override val sortType = SortType.WHITMORE_SCORE
            override val sortBy = PublisherDao.SortType.WHITMORE_SCORE
            override fun getSectionHeader(publisher: CompanyEntity?): String {
                return (publisher?.whitmoreScore ?: 0).orderOfMagnitude()
            }
        }
    }
}
