package com.boardgamegeek.livedata

import androidx.paging.PositionalDataSource
import com.boardgamegeek.entities.ThreadEntity
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract
import timber.log.Timber
import java.io.IOException

class ForumDataSource(private val forumId: Int) : PositionalDataSource<ThreadEntity>() {
    private val bggService = Adapter.createForXml()
    private var totalCount: Int = 0
    private val pageSize = ForumResponse.PAGE_SIZE

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<ThreadEntity>) {
        if (forumId == BggContract.INVALID_ID) return
        try {
            val response = bggService.forum(forumId, 1).execute()
            if (response.isSuccessful) {
                totalCount = response.body()?.numthreads?.toIntOrNull() ?: 0
                callback.onResult(response.body()?.threads.orEmpty().mapToEntity(), 0, totalCount)
            } else {
                Timber.w("Error code: ${response.code()}\n${response.errorBody()}")
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ThreadEntity>) {
        if (params.loadSize <= 0) return
        if (params.startPosition >= totalCount) return
        val page = (params.startPosition + pageSize) / pageSize
        if (page <= 0) return

        try {
            val response = bggService.forum(forumId, page).execute()
            if (response.isSuccessful) {
                val threads = response.body()?.threads.orEmpty()
                callback.onResult(threads.mapToEntity())
            } else {
                Timber.w("Error code: ${response.code()}\n${response.errorBody()}")
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
    }
}
