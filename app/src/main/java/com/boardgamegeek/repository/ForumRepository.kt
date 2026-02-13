package com.boardgamegeek.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.ForumResponse
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.Forum
import com.boardgamegeek.util.ForumXmlApiMarkupConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.boardgamegeek.model.Thread
import com.boardgamegeek.provider.BggContract
import retrofit2.HttpException
import timber.log.Timber

class ForumRepository(
    context: Context,
    private val api: BggService,
) {
    private val converter: ForumXmlApiMarkupConverter = ForumXmlApiMarkupConverter(context.getString(R.string.spoiler))

    suspend fun loadForGame(gameId: Int): List<Forum> = loadForums(BggService.ForumType.THING, gameId)

    suspend fun loadForPerson(personId: Int): List<Forum> = loadForums(BggService.ForumType.PERSON, personId)

    suspend fun loadForCompany(companyId: Int): List<Forum> = loadForums(BggService.ForumType.COMPANY, companyId)

    suspend fun loadForRegion(region: BggService.ForumRegion = BggService.ForumRegion.BOARDGAME): List<Forum> =
        loadForums(BggService.ForumType.REGION, region.id)

    private suspend fun loadForums(type: BggService.ForumType, id: Int): List<Forum> = withContext(Dispatchers.IO) {
        val response = api.forumList(type, id)
        response.forums.map { it.mapToModel() }
    }

    fun loadPager(forumId: Int): Pager<Int, Thread> = Pager(PagingConfig(ForumResponse.PAGE_SIZE)) {
        ForumPagingSource(forumId, api)
    }

    suspend fun loadThread(threadId: Int) = withContext(Dispatchers.IO) {
        val response = api.thread(threadId)
        response.mapToModel(converter)
    }

    class ForumPagingSource(private val forumId: Int, private val api: BggService) : PagingSource<Int, Thread>() {
        override fun getRefreshKey(state: PagingState<Int, Thread>): Int? {
            return null
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Thread> {
            return try {
                if (forumId == BggContract.INVALID_ID) return LoadResult.Error(Exception("Invalid Forum ID"))

                val currentPage = params.key ?: 1
                val response = withContext(Dispatchers.IO) {
                    api.forum(forumId, currentPage)
                }
                val forum = response.mapToModel()
                val nextPage = if (currentPage * ForumResponse.PAGE_SIZE < forum.numberOfThreads) currentPage + 1 else null
                LoadResult.Page(forum.threads, null, nextPage)
            } catch (e: Exception) {
                if (e is HttpException) {
                    Timber.w("Error code: ${e.code()}\n${e.response()?.body()}")
                } else {
                    Timber.w(e)
                }
                LoadResult.Error(e)
            }
        }
    }
}
