package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.Forum
import com.boardgamegeek.model.ForumThreads
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.util.ForumXmlApiMarkupConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun loadForum(forumId: Int, page: Int = 1): ForumThreads = withContext(Dispatchers.IO) {
        val response = api.forum(forumId, page)
        response.mapToModel()
    }

    suspend fun loadThread(threadId: Int) = withContext(Dispatchers.IO) {
        val response = api.thread(threadId)
        response.mapToModel(converter)
    }
}
