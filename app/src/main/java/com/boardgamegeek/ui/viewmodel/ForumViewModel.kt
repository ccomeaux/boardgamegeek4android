package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.boardgamegeek.livedata.ForumDataSource
import com.boardgamegeek.provider.BggContract

class ForumViewModel(application: Application) : AndroidViewModel(application) {
    private val _forumId = MutableLiveData<Int>()
    val forumId: LiveData<Int>
        get() = _forumId

    fun setForumId(id: Int) {
        if (_forumId.value != id) _forumId.value = id
    }

    private var dataSourceFactory: DataSource.Factory<Int, com.boardgamegeek.model.Thread> = ForumDataSourceFactory(BggContract.INVALID_ID)

    private val config = PagedList.Config.Builder()
            .setPageSize(50)
            .setInitialLoadSizeHint(50)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(false)
            .build()

    val threads: LiveData<PagedList<com.boardgamegeek.model.Thread>> = Transformations.switchMap(_forumId) {
        // TODO map to entity
        dataSourceFactory = ForumDataSourceFactory(it)
        LivePagedListBuilder(dataSourceFactory, config).build()
    }

    class ForumDataSourceFactory(private val forumId: Int) : DataSource.Factory<Int, com.boardgamegeek.model.Thread>() {
        override fun create(): DataSource<Int, com.boardgamegeek.model.Thread> {
            return ForumDataSource(forumId)
        }
    }
}
