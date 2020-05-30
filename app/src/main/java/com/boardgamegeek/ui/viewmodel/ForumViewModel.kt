package com.boardgamegeek.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.boardgamegeek.entities.ThreadEntity
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.io.model.ForumThread
import com.boardgamegeek.livedata.ForumDataSource
import com.boardgamegeek.provider.BggContract
import java.text.SimpleDateFormat
import java.util.*

class ForumViewModel(application: Application) : AndroidViewModel(application) {
    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    private val _forumId = MutableLiveData<Int>()

    fun setForumId(id: Int) {
        if (_forumId.value != id) _forumId.value = id
    }

    private var dataSourceFactory: DataSource.Factory<Int, ForumThread> = ForumDataSourceFactory(BggContract.INVALID_ID)

    private val config = PagedList.Config.Builder()
            .setPageSize(ForumResponse.PAGE_SIZE)
            .setInitialLoadSizeHint(ForumResponse.PAGE_SIZE)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(true)
            .build()

    val threads: LiveData<PagedList<ThreadEntity>> = Transformations.switchMap(_forumId) {
        dataSourceFactory = ForumDataSourceFactory(it)
        LivePagedListBuilder(dataSourceFactory.map { thread ->
            ThreadEntity(
                    thread.id,
                    thread.subject.orEmpty().trim(),
                    thread.author.orEmpty().trim(),
                    thread.numarticles,
                    dateFormat.parse(thread.lastpostdate.orEmpty())?.time ?: 0L
            )
        }, config).build()
    }

    class ForumDataSourceFactory(private val forumId: Int) : DataSource.Factory<Int, ForumThread>() {
        override fun create(): DataSource<Int, ForumThread> {
            return ForumDataSource(forumId)
        }
    }
}
