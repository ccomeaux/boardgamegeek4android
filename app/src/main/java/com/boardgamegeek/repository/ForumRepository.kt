package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.io.model.ForumListResponse
import com.boardgamegeek.livedata.NetworkLoader
import com.boardgamegeek.mappers.ForumMapper
import retrofit2.Call
import java.util.*

class ForumRepository(val application: BggApplication) {
    fun getForumsForGame(gameId: Int): LiveData<RefreshableResource<List<ForumEntity>>> {
        return object : NetworkLoader<List<ForumEntity>, ForumListResponse>(application) {
            override val typeDescriptionResId: Int
                get() = R.string.title_forums

            override fun createCall(): Call<ForumListResponse> {
                return Adapter.createForXml().forumList(BggService.FORUM_TYPE_THING, gameId)
            }

            override fun parseResult(result: ForumListResponse): List<ForumEntity> {
                return mapForums(result)
            }

        }.asLiveData()
    }

    fun getForumsForPerson(personId: Int): LiveData<RefreshableResource<List<ForumEntity>>> {
        return object : NetworkLoader<List<ForumEntity>, ForumListResponse>(application) {
            override val typeDescriptionResId: Int
                get() = R.string.title_forums

            override fun createCall(): Call<ForumListResponse> {
                return Adapter.createForXml().forumList(BggService.FORUM_TYPE_PERSON, personId)
            }

            override fun parseResult(result: ForumListResponse): List<ForumEntity> {
                return mapForums(result)
            }

        }.asLiveData()
    }

    fun getForums(): LiveData<RefreshableResource<List<ForumEntity>>> {
        return object : NetworkLoader<List<ForumEntity>, ForumListResponse>(application) {
            override val typeDescriptionResId: Int
                get() = R.string.title_forums

            override fun createCall(): Call<ForumListResponse> {
                return Adapter.createForXml().forumList(BggService.FORUM_TYPE_REGION, BggService.FORUM_REGION_BOARDGAME)
            }

            override fun parseResult(result: ForumListResponse): List<ForumEntity> {
                return mapForums(result)
            }

        }.asLiveData()
    }

    private fun mapForums(result: ForumListResponse): List<ForumEntity> {
        val forums = ArrayList<ForumEntity>()
        val mapper = ForumMapper()
        result.forums?.forEach {
            forums.add(mapper.map(it))
        }
        return forums
    }
}