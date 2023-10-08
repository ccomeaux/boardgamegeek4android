package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.GameComments
import com.boardgamegeek.entities.Game
import com.boardgamegeek.entities.GamePoll
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.*
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
    private val collectionDao = CollectionDao(context)

    suspend fun loadGame(gameId: Int) = dao.load(gameId)?.mapToModel()

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

    suspend fun fetchGame(vararg gameId: Int): List<Game> = withContext(Dispatchers.IO) {
        val response = if (gameId.size == 1) {
            api.thing(gameId.first(), 1)
        } else {
            api.things(gameId.joinToString(), 1)
        }
        response.games.map { it.mapToModel() }
    }

    suspend fun refreshHeroImage(game: Game): Game = withContext(Dispatchers.IO) {
        val urlMap = imageRepository.getImageUrls(game.thumbnailUrl.getImageId())
        val urls = urlMap[ImageRepository.ImageType.HERO]
        urls?.firstOrNull()?.let { url ->
            dao.updateHeroUrl(game.id, url)
            game.copy(heroImageUrl = url)
        } ?: game
    }

    suspend fun loadComments(gameId: Int, page: Int): GameComments? = withContext(Dispatchers.IO) {
        val response = api.thingWithComments(gameId, page)
        response.games.firstOrNull()?.mapToRatingModel()
    }

    suspend fun loadRatings(gameId: Int, page: Int): GameComments? = withContext(Dispatchers.IO) {
        val response = api.thingWithRatings(gameId, page)
        response.games.firstOrNull()?.mapToRatingModel()
    }

    suspend fun getRanks(gameId: Int) = dao.loadRanks(gameId).map { it.mapToModel() }

    suspend fun getLanguagePoll(gameId: Int) = GamePoll(dao.loadPoll(gameId, GameDao.PollType.LANGUAGE_DEPENDENCE).map { it.mapToModel() })

    suspend fun getAgePoll(gameId: Int) = GamePoll(dao.loadPoll(gameId, GameDao.PollType.SUGGESTED_PLAYER_AGE).map { it.mapToModel() })

    suspend fun getPlayerPoll(gameId: Int) = dao.loadPlayerPoll(gameId).map { it.mapToModel() }

    suspend fun getDesigners(gameId: Int) = dao.loadDesigners(gameId).map { it.mapToGameDetail() }

    suspend fun getArtists(gameId: Int) = dao.loadArtists(gameId).map { it.mapToGameDetail() }

    suspend fun getPublishers(gameId: Int) = dao.loadPublishers(gameId).map { it.mapToGameDetail() }

    suspend fun getCategories(gameId: Int) = dao.loadCategories(gameId).map { it.mapToGameDetail() }

    suspend fun getMechanics(gameId: Int) = dao.loadMechanics(gameId).map { it.mapToGameDetail() }

    suspend fun getExpansions(gameId: Int) = dao.loadExpansions(gameId).map { entity ->
        val items = collectionDao.loadByGame(entity.expansionId).map { it.second.mapToModel(it.first.mapToModel()) }
        entity.mapToModel(items)
    }

    suspend fun getBaseGames(gameId: Int) = dao.loadExpansions(gameId, true).map { entity ->
        val items = collectionDao.loadByGame(entity.expansionId).map { it.second.mapToModel(it.first.mapToModel()) }
        entity.mapToModel(items)
    }

    suspend fun getPlays(gameId: Int) = playDao.loadPlaysByGame(gameId).map { it.mapToModel() }

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
