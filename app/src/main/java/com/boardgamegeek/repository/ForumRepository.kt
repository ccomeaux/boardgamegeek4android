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

    suspend fun loadForGame(gameId: Int): List<ForumEntity> = loadForums(BggService.ForumType.THING, gameId)

    suspend fun loadForPerson(personId: Int): List<ForumEntity> = loadForums(BggService.ForumType.PERSON, personId)

    suspend fun loadForCompany(companyId: Int): List<ForumEntity> = loadForums(BggService.ForumType.COMPANY, companyId)

    suspend fun loadForRegion(region: BggService.ForumRegion = BggService.ForumRegion.BOARDGAME): List<ForumEntity> =
        loadForums(BggService.ForumType.REGION, region.id)

    private suspend fun loadForums(type: BggService.ForumType, id: Int): List<ForumEntity> = withContext(Dispatchers.IO) {
        val response = Adapter.createForXml().forumList(type.id, id)
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
