package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.firstChar
import com.boardgamegeek.extensions.orderOfMagnitude
import com.boardgamegeek.repository.ArtistRepository

class ArtistsViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    private val artistRepository = ArtistRepository(getApplication())

    private val _sort = MutableLiveData<ArtistsSort>()
    val sort: LiveData<ArtistsSort>
        get() = _sort

    init {
        sort(SortType.ITEM_COUNT)
    }

    val artists: LiveData<List<PersonEntity>> = Transformations.switchMap(sort) {
        artistRepository.loadArtists(it.sortBy)
    }

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> ArtistsSortByName()
            SortType.ITEM_COUNT -> ArtistsSortByItemCount()
        }
    }

    fun getSectionHeader(artist: PersonEntity?): String {
        return sort.value?.getSectionHeader(artist) ?: ""
    }
}

sealed class ArtistsSort {
    abstract val sortType: ArtistsViewModel.SortType
    abstract val sortBy: ArtistDao.SortType
    abstract fun getSectionHeader(artist: PersonEntity?): String
}

class ArtistsSortByName : ArtistsSort() {
    override val sortType = ArtistsViewModel.SortType.NAME
    override val sortBy = ArtistDao.SortType.NAME
    override fun getSectionHeader(artist: PersonEntity?): String {
        return if (artist?.name == "(Uncredited)") "-"
        else artist?.name.firstChar()
    }
}

class ArtistsSortByItemCount : ArtistsSort() {
    override val sortType = ArtistsViewModel.SortType.ITEM_COUNT
    override val sortBy = ArtistDao.SortType.ITEM_COUNT
    override fun getSectionHeader(artist: PersonEntity?): String {
        return (artist?.itemCount ?: 0).orderOfMagnitude()
    }
}