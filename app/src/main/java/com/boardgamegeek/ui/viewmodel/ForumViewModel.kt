package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.livedata.ForumPagingSource
import com.boardgamegeek.repository.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    application: Application,
    private val repository: ForumRepository
) : AndroidViewModel(application) {
    private val _forumId = MutableLiveData<Int>()

    fun setForumId(id: Int) {
        if (_forumId.value != id) _forumId.value = id
    }

    val threads = _forumId.switchMap { forumId ->
        Pager(
            PagingConfig(
                pageSize = ForumResponse.PAGE_SIZE,
                initialLoadSize = ForumResponse.PAGE_SIZE,
                prefetchDistance = 30,
                enablePlaceholders = true,
            )
        ) {
            ForumPagingSource(forumId, repository)
        }.liveData.cachedIn(viewModelScope)
    }
}
