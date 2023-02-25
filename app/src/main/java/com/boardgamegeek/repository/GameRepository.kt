package com.boardgamegeek.repository

import android.content.Context
import androidx.core.content.contentValuesOf
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.GameCommentsEntity
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.mappers.mapToRatingEntities
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.provider.BggContract.Games
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class GameRepository @Inject constructor(
    val context: Context,
    private val api: BggService,
) {
    private val dao = GameDao(context)
    private val playDao = PlayDao(context)

    suspend fun loadGame(gameId: Int) = dao.load(gameId)

    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int = 0) = dao.loadOldestUpdatedGames(gamesPerFetch)

    suspend fun loadUnupdatedGames(gamesPerFetch: Int = 0) = dao.loadUnupdatedGames(gamesPerFetch)

    suspend fun loadDeletableGames(hoursAgo: Long, includeUnplayedGames: Boolean) = dao.loadDeletableGames(hoursAgo, includeUnplayedGames)

    suspend fun refreshGame(gameId: Int) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = api.thing(gameId, 1)
        response.games.firstOrNull()?.let { game ->
            dao.save(game.mapToEntity(), timestamp)
            Timber.i("Synced game '$gameId'")
        }
    }

    suspend fun saveGame(entity: GameEntity, timestamp: Long) = withContext(Dispatchers.IO) {
        if (entity.name.isBlank()) {
            dao.delete(entity.id)
            Timber.i("Deleted game '${entity.id}' while attempting to save")
        } else {
            dao.save(entity, timestamp)
            Timber.i("Saved game '${entity.id}'")
        }
    }

    suspend fun refreshHeroImage(game: GameEntity): GameEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image(game.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.update(game.id, contentValuesOf(Games.Columns.HERO_IMAGE_URL to url))
        game.copy(heroImageUrl = url)
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

    suspend fun getPlays(gameId: Int) = playDao.loadPlaysByGame(gameId)

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
        val colors = playDao.loadPlayerColors(gameId)
        dao.insertColors(gameId, colors)
    }

    suspend fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) {
        if (gameId != INVALID_ID) {
            dao.update(gameId, contentValuesOf(Games.Columns.LAST_VIEWED to lastViewed))
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
            val values = contentValuesOf(
                Games.Columns.ICON_COLOR to iconColor,
                Games.Columns.DARK_COLOR to darkColor,
                Games.Columns.WINS_COLOR to winsColor,
                Games.Columns.WINNABLE_PLAYS_COLOR to winnablePlaysColor,
                Games.Columns.ALL_PLAYS_COLOR to allPlaysColor,
            )
            val numberOfRowsModified = dao.update(gameId, values)
            Timber.d(numberOfRowsModified.toString())
        }
    }

    suspend fun updateColors(gameId: Int, colors: List<String>) = dao.updateColors(gameId, colors)

    suspend fun updateFavorite(gameId: Int, isFavorite: Boolean) {
        if (gameId != INVALID_ID) {
            dao.update(gameId, contentValuesOf(Games.Columns.STARRED to if (isFavorite) 1 else 0))
        }
    }

    suspend fun delete(gameId: Int) = dao.delete(gameId)

    suspend fun delete() = dao.delete()
}
