package com.boardgamegeek.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.boardgamegeek.db.ArtistDao
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.repository.ArtistRepository

class ArtistsViewModel(application: Application) : AndroidViewModel(application) {
    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    private val artistRepository = ArtistRepository(getApplication())
    private val prefs: SharedPreferences by lazy { application.preferences() }

    private val _sort = MutableLiveData<ArtistsSort>()
    val sort: LiveData<ArtistsSort>
        get() = _sort

    init {
        val initialSort = if (prefs.isStatusSetToSync(COLLECTION_STATUS_RATED))
            SortType.WHITMORE_SCORE
        else
            SortType.ITEM_COUNT
        sort(initialSort)
    }

    val artists: LiveData<List<PersonEntity>> = Transformations.switchMap(sort) {
        artistRepository.loadArtists(it.sortBy)
    }

    val progress: LiveData<Pair<Int, Int>> = artistRepository.progress

    fun sort(sortType: SortType) {
        _sort.value = when (sortType) {
            SortType.NAME -> ArtistsSortByName()
            SortType.ITEM_COUNT -> ArtistsSortByItemCount()
            SortType.WHITMORE_SCORE -> ArtistsSortByWhitmoreScore()
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

class ArtistsSortByWhitmoreScore : ArtistsSort() {
    override val sortType = ArtistsViewModel.SortType.WHITMORE_SCORE
    override val sortBy = ArtistDao.SortType.WHITMORE_SCORE
    override fun getSectionHeader(artist: PersonEntity?): String {
        return (artist?.whitmoreScore ?: 0).orderOfMagnitude()
    }
}
