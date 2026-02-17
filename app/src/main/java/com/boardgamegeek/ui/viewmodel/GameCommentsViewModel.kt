package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GameCommentsViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
) : AndroidViewModel(application) {
    enum class SortType {
        RATING, USER
    }

    private val _id = MutableLiveData<Pair<Int, SortType>>()

    val sort: LiveData<SortType> = _id.map {
        _id.value?.second ?: SortType.USER
    }

    fun setGameId(id: Int) {
        if (_id.value?.first != id) _id.value = id to (_id.value?.second ?: SortType.RATING)
    }

    fun setSort(sort: SortType) {
        if (_id.value?.second != sort) _id.value = (_id.value?.first ?: BggContract.INVALID_ID) to sort
    }

    val comments = _id.switchMap {
        gameRepository.loadCommentsPager(
            it.first,
            it.second == SortType.RATING
        ).liveData.cachedIn(this)
    }
}
