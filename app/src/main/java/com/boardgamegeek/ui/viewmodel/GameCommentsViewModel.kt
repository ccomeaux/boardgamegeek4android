@file:OptIn(ExperimentalCoroutinesApi::class)

package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.boardgamegeek.model.GameComment
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class GameCommentsViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
) : AndroidViewModel(application) {
    private val gameId = MutableStateFlow(BggContract.INVALID_ID)
    private val _sort = MutableStateFlow(GameComment.SortType.Comment)
    val sort = _sort.asStateFlow()

    fun setGameId(id: Int) {
        if (id != gameId.value) gameId.value = id
    }

    fun setSort(sort: GameComment.SortType) {
        if (sort != _sort.value) _sort.value = sort
    }

    val comments = combine(gameId, sort) { gameId, sortType ->
        gameId to sortType
    }.flatMapLatest {
        gameRepository.loadCommentsPager(
            it.first,
            it.second == GameComment.SortType.Rating
        ).flow.cachedIn(viewModelScope)
    }
}
