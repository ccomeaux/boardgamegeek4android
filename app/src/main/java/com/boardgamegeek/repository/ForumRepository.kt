package com.boardgamegeek.repository

import android.app.Application
import com.boardgamegeek.R
import com.boardgamegeek.entities.ForumEntity
import com.boardgamegeek.entities.ForumThreadsEntity
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.util.ForumXmlApiMarkupConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForumRepository(application: Application) {
    private val converter: ForumXmlApiMarkupConverter = ForumXmlApiMarkupConverter(application.getString(R.string.spoiler))

    suspend fun loadForGame(gameId: Int): List<ForumEntity> = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().forumList(BggService.FORUM_TYPE_THING, gameId)
        response.mapToEntity()
    }

    suspend fun loadForPerson(personId: Int): List<ForumEntity> = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().forumList(BggService.FORUM_TYPE_PERSON, personId)
        response.mapToEntity()
    }

    suspend fun loadForCompany(companyId: Int): List<ForumEntity> = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().forumList(BggService.FORUM_TYPE_COMPANY, companyId)
        response.mapToEntity()
    }

    suspend fun loadForRegion(regionId: Int = BggService.FORUM_REGION_BOARDGAME): List<ForumEntity> = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().forumList(BggService.FORUM_TYPE_REGION, regionId)
        response.mapToEntity()
    }

    suspend fun loadForum(forumId: Int, page: Int = 1): ForumThreadsEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().forum(forumId, page)
        response.mapToEntity()
    }

    suspend fun loadThread(threadId: Int) = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().thread(threadId)
        response.mapToEntity(converter)
    }
}
