@file:OptIn(ExperimentalCoroutinesApi::class)

package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.repository.ForumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    application: Application,
    private val repository: ForumRepository
) : AndroidViewModel(application) {
    private val _forumId = MutableStateFlow(BggContract.INVALID_ID)

    fun setForumId(id: Int) {
        if (_forumId.value != id) _forumId.value = id
    }

    val threads = _forumId.flatMapLatest { forumId ->
        repository.loadPager(forumId).flow.cachedIn(viewModelScope)
    }
}
