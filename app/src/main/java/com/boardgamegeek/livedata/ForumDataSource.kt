package com.boardgamegeek.livedata

import androidx.paging.PositionalDataSource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.model.Thread

class ForumDataSource(private val forumId: Int) : PositionalDataSource<Thread>() {
    private val bggService = Adapter.createForXml()
    private var totalCount: Int = 0
    private val pageSize = 50

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Thread>) {
        val forum = bggService.forum(forumId, 1).execute().body()
        totalCount = forum?.numberOfThreads() ?: 0
        // TODO handle failure
        callback.onResult(forum?.threads.orEmpty(), 0)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Thread>) {
        if (params.loadSize <= 0) return
        val page = (params.startPosition + pageSize - 1) / pageSize
        if (page <= 0) return
        val forum = bggService.forum(forumId, page).execute().body()
        callback.onResult(forum?.threads.orEmpty())
    }
}