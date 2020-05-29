package com.boardgamegeek.livedata

import androidx.paging.PositionalDataSource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.io.model.ForumThread

class ForumDataSource(private val forumId: Int) : PositionalDataSource<ForumThread>() {
    private val bggService = Adapter.createForXml()
    private var totalCount: Int = 0
    private val pageSize = ForumResponse.PAGE_SIZE

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<ForumThread>) {
        val forum = bggService.forum(forumId, 1).execute().body()
        totalCount = forum?.numthreads?.toIntOrNull() ?: 0
        // TODO handle failure
        callback.onResult(forum?.threads.orEmpty(), 0)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ForumThread>) {
        if (params.loadSize <= 0) return
        if (params.startPosition == totalCount) return
        val page = (params.startPosition + pageSize - 1) / pageSize
        if (page <= 0) return
        val forum = bggService.forum(forumId, page).execute().body()
        callback.onResult(forum?.threads.orEmpty())
    }
}