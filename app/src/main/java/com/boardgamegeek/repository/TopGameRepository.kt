package com.boardgamegeek.repository

import com.boardgamegeek.model.TopGame
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class TopGameRepository {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun findTopGames(): List<TopGame> = withContext(Dispatchers.IO) {
        var rank = 1
        val doc = Jsoup
            .connect("https://www.boardgamegeek.com/browse/boardgame")
            .timeout(10_000)
            .get()
        val gameElements = doc.select("td.collection_thumbnail")
        gameElements.map { element ->
            val link = element.getElementsByTag("a").first()

            val id = link?.attr("href")?.substringAfter("/boardgame/")?.substringAfter("/boardgameexpansion/")?.substringBefore("/")?.toIntOrNull()
                ?: INVALID_ID

            @Suppress("SpellCheckingInspection")
            val gameNameElement = element.parent()?.select(".collection_objectname")?.getOrNull(0)?.child(1)
            val yearPublishedText =
                gameNameElement?.child(1)?.text().orEmpty().trimStart('(').trimEnd(')').toIntOrNull() ?: TopGame.YEAR_UNKNOWN
            val thumbnailUrl = link?.child(0)?.attr("src").orEmpty()

            TopGame(
                id,
                gameNameElement?.child(0)?.text().orEmpty(),
                rank++,
                yearPublishedText,
                thumbnailUrl,
            )
        }
    }
}
