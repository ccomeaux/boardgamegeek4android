package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.db.PublisherDao
import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.repository.PublisherRepository

class PublishersViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    private val publisherRepository = PublisherRepository(getApplication())

    private val _sort = MutableLiveData<PublishersSort>()
    val sort: LiveData<PublishersSort>
        get() = _sort

    init {
        sort(SortType.ITEM_COUNT)
    }

    val publishers: LiveData<List<CompanyEntity>> = Transformations.switchMap(sort) {
        publisherRepository.loadPublishers(it.sortBy)
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> PublishersSortByName()
            SortType.ITEM_COUNT -> PublishersSortByItemCount()
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
        return if (publisher?.name == "(Uncredited)") "-"
        else publisher?.name.firstChar()
    }
}

class PublishersSortByItemCount : PublishersSort() {
    override val sortType = PublishersViewModel.SortType.ITEM_COUNT
    override val sortBy = PublisherDao.SortType.ITEM_COUNT
    override fun getSectionHeader(publisher: CompanyEntity?): String {
        return (publisher?.itemCount ?: 0).orderOfMagnitude()
    }
}
