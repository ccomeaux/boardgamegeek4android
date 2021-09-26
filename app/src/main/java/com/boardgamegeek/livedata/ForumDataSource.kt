package com.boardgamegeek.livedata

import androidx.paging.PositionalDataSource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.io.model.ForumThread
import timber.log.Timber
import java.io.IOException

class ForumDataSource(private val forumId: Int) : PositionalDataSource<ForumThread>() {
    private val bggService = Adapter.createForXml()
    private var totalCount: Int = 0
    private val pageSize = ForumResponse.PAGE_SIZE

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<ForumThread>) {
        try {
            val response = bggService.forum(forumId, 1).execute()
            if (response.isSuccessful) {
                totalCount = response.body()?.numthreads?.toIntOrNull() ?: 0
                callback.onResult(response.body()?.threads.orEmpty(), 0, totalCount)
            } else {
                Timber.w("Error code: ${response.code()}\n${response.errorBody()}")
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ForumThread>) {
        if (params.loadSize <= 0) return
        if (params.startPosition >= totalCount) return
        val page = (params.startPosition + pageSize) / pageSize
        if (page <= 0) return

        try {
            val response = bggService.forum(forumId, page).execute()
            if (response.isSuccessful) {
                callback.onResult(response.body()?.threads.orEmpty())
            } else {
                Timber.w("Error code: ${response.code()}\n${response.errorBody()}")
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
    }
}