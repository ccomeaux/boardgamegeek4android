package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.GameCommentsEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapForUpsert
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.mappers.mapToRatingEntities
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class GameRepository @Inject constructor(
    val context: Context,
    private val api: BggService,
    private val imageRepository: ImageRepository,
) {
    private val dao = GameDao(context)
    private val playDao = PlayDao(context)

    suspend fun loadGame(gameId: Int) = dao.load(gameId)?.mapToEntity()

    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int = 0) = dao.loadOldestUpdatedGames(gamesPerFetch)

    suspend fun loadUnupdatedGames(gamesPerFetch: Int = 0) = dao.loadUnupdatedGames(gamesPerFetch)

    suspend fun loadDeletableGames(hoursAgo: Long, includeUnplayedGames: Boolean) = dao.loadDeletableGames(hoursAgo, includeUnplayedGames)

    suspend fun refreshGame(vararg gameId: Int): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val games = api.thing(gameId.first(), 1).games
        games?.forEach { game ->
            val gameForUpsert = game.mapForUpsert(timestamp)
            dao.save(gameForUpsert)
            Timber.d("Synced game $gameForUpsert")
        }
        games?.size ?: 0
    }

    suspend fun fetchGame(vararg gameId: Int): List<GameEntity> = withContext(Dispatchers.IO) {
        val response = if (gameId.size == 1) {
            api.thing(gameId.first(), 1)
        } else {
            api.things(gameId.joinToString(), 1)
        }
        response.games.map { it.mapToEntity() }
    }

    suspend fun refreshHeroImage(game: GameEntity): GameEntity = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(game.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let { url ->
            dao.updateHeroUrl(game.id, url)
            game.copy(heroImageUrl = url)
        } ?: game
    }

    suspend fun loadComments(gameId: Int, page: Int): GameCommentsEntity? = withContext(Dispatchers.IO) {
        val response = api.thingWithComments(gameId, page)
        response.games.firstOrNull()?.mapToRatingEntities()
    }

    suspend fun loadRatings(gameId: Int, page: Int): GameCommentsEntity? = withContext(Dispatchers.IO) {
        val response = api.thingWithRatings(gameId, page)
        response.games.firstOrNull()?.mapToRatingEntities()
    }

    suspend fun getRanks(gameId: Int) = dao.loadRanks(gameId)

    suspend fun getLanguagePoll(gameId: Int) = dao.loadPoll(gameId, GameDao.PollType.LANGUAGE_DEPENDENCE)

    suspend fun getAgePoll(gameId: Int) = dao.loadPoll(gameId, GameDao.PollType.SUGGESTED_PLAYER_AGE)

    suspend fun getPlayerPoll(gameId: Int) = dao.loadPlayerPoll(gameId)

    suspend fun getDesigners(gameId: Int) = dao.loadDesigners(gameId)

    suspend fun getArtists(gameId: Int) = dao.loadArtists(gameId)

    suspend fun getPublishers(gameId: Int) = dao.loadPublishers(gameId)

    suspend fun getCategories(gameId: Int) = dao.loadCategories(gameId)

    suspend fun getMechanics(gameId: Int) = dao.loadMechanics(gameId)

    suspend fun getExpansions(gameId: Int) = dao.loadExpansions(gameId)

    suspend fun getBaseGames(gameId: Int) = dao.loadExpansions(gameId, true)

    suspend fun getPlays(gameId: Int) = playDao.loadPlaysByGame(gameId).map { it.mapToEntity() }

    /**
     * Returns a map of all game IDs with player colors.
     */
    suspend fun getPlayColors() = dao.loadPlayColors().groupBy({ it.first }, { it.second })

    suspend fun getPlayColors(gameId: Int) = dao.loadPlayColors(gameId)

    suspend fun addPlayColor(gameId: Int, color: String?) {
        if (gameId != INVALID_ID && !color.isNullOrBlank()) {
            dao.insertColor(gameId, color)
        }
    }

    suspend fun deletePlayColor(gameId: Int, color: String): Int {
        return if (gameId != INVALID_ID && color.isNotBlank()) {
            dao.deleteColor(gameId, color)
        } else 0
    }

    suspend fun computePlayColors(gameId: Int) {
        val colors = playDao.loadPlayerColorsForGame(gameId)
        dao.insertColors(gameId, colors)
    }

    suspend fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) {
        if (gameId != INVALID_ID) {
            dao.updateLastViewed(gameId, lastViewed)
        }
    }

    suspend fun updateGameColors(
        gameId: Int,
        iconColor: Int,
        darkColor: Int,
        winsColor: Int,
        winnablePlaysColor: Int,
        allPlaysColor: Int
    ) {
        if (gameId != INVALID_ID) {
            dao.updateGameColors(gameId, iconColor, darkColor, winsColor, winnablePlaysColor, allPlaysColor)
        }
    }

    suspend fun updateColors(gameId: Int, colors: List<String>) = dao.updateColors(gameId, colors)

    suspend fun updateFavorite(gameId: Int, isFavorite: Boolean) {
        if (gameId != INVALID_ID) dao.updateStarred(gameId, isFavorite)
    }

    suspend fun delete(gameId: Int) = dao.delete(gameId)

    suspend fun delete() = dao.delete()
}
