package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.PublisherRepository

class PublishersViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    private val publisherRepository = PublisherRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }

    private val _sort = MutableLiveData<PublishersSort>()
    val sort: LiveData<PublishersSort>
        get() = _sort

    init {
        val initialSort = if (prefs.isStatusSetToSync(COLLECTION_STATUS_RATED))
            SortType.WHITMORE_SCORE
        else
            SortType.ITEM_COUNT
        sort(initialSort)
    }

    val publishers: LiveData<List<CompanyEntity>> = Transformations.switchMap(sort) {
        publisherRepository.loadPublishers(it.sortBy)
    }

    val progress: LiveData<Pair<Int, Int>> = publisherRepository.progress

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> PublishersSortByName()
            SortType.ITEM_COUNT -> PublishersSortByItemCount()
            SortType.WHITMORE_SCORE -> PublishersSortByWhitmoreScore()
        }
    }

    fun getSectionHeader(publisher: CompanyEntity?): String {
        return sort.value?.getSectionHeader(publisher) ?: ""
    }
}

sealed class PublishersSort {
    abstract val sortType: PublishersViewModel.SortType
    abstract val sortBy: PublisherDao.SortType
    abstract fun getSectionHeader(publisher: CompanyEntity?): String
}

class PublishersSortByName : PublishersSort() {
    override val sortType = PublishersViewModel.SortType.NAME
    override val sortBy = PublisherDao.SortType.NAME
    override fun getSectionHeader(publisher: CompanyEntity?): String {
        return when {
            publisher == null -> ""
            publisher.name.startsWith("(") -> "-"
            else -> publisher.name.firstChar()
        }
    }
}

class PublishersSortByItemCount : PublishersSort() {
    override val sortType = PublishersViewModel.SortType.ITEM_COUNT
    override val sortBy = PublisherDao.SortType.ITEM_COUNT
    override fun getSectionHeader(publisher: CompanyEntity?): String {
        return (publisher?.itemCount ?: 0).orderOfMagnitude()
    }
}

class PublishersSortByWhitmoreScore : PublishersSort() {
    override val sortType = PublishersViewModel.SortType.WHITMORE_SCORE
    override val sortBy = PublisherDao.SortType.WHITMORE_SCORE
    override fun getSectionHeader(publisher: CompanyEntity?): String {
        return (publisher?.whitmoreScore ?: 0).orderOfMagnitude()
    }
}
