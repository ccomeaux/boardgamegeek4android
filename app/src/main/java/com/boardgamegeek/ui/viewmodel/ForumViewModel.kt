package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.livedata.ForumDataSource
import com.boardgamegeek.repository.ForumRepository

class ForumViewModel(application: Application) : AndroidViewModel(application) {
    private val _forumId = MutableLiveData<Int>()

    fun setForumId(id: Int) {
        if (_forumId.value != id) _forumId.value = id
    }

    val threads = _forumId.switchMap {
        Pager(
                PagingConfig(
                        pageSize = ForumResponse.PAGE_SIZE,
                        initialLoadSize = ForumResponse.PAGE_SIZE,
                        prefetchDistance = 30,
                        enablePlaceholders = true,
                )
        ) {
            ForumDataSource(it, ForumRepository(application))
        }.flow.asLiveData()
    }
}
