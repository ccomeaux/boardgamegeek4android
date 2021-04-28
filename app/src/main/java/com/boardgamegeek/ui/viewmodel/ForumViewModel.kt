package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.boardgamegeek.entities.ThreadEntity
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.livedata.ForumDataSource
import com.boardgamegeek.provider.BggContract

class ForumViewModel(application: Application) : AndroidViewModel(application) {
    private val _forumId = MutableLiveData<Int>()

    fun setForumId(id: Int) {
        if (_forumId.value != id) _forumId.value = id
    }

    private val config = PagedList.Config.Builder()
            .setPageSize(ForumResponse.PAGE_SIZE)
            .setInitialLoadSizeHint(ForumResponse.PAGE_SIZE)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(true)
            .build()

    val threads: LiveData<PagedList<ThreadEntity>> = _forumId.switchMap {
        when (it){
            BggContract.INVALID_ID -> AbsentLiveData.create()
            else -> LivePagedListBuilder(ForumDataSourceFactory(it), config).build()
        }
    }

    class ForumDataSourceFactory(private val forumId: Int) : DataSource.Factory<Int, ThreadEntity>() {
        override fun create(): DataSource<Int, ThreadEntity> {
            return ForumDataSource(forumId)
        }
    }
}
