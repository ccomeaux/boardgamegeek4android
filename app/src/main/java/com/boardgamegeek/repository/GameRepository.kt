package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.extensions.isOlderThan
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.util.ImageUtils.getImageId
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GameRepository(val application: BggApplication) {
    private val dao = GameDao(application)
    private val playDao = PlayDao(application)
    private val bggService = Adapter.createForXml()
    private val refreshPlaysPartialMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_PARTIAL_MINUTES)
    private val refreshPlaysFullHours = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_FULL_HOURS)
    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    suspend fun loadGame(gameId: Int) = dao.load(gameId)

    suspend fun refreshGame(gameId: Int): GameEntity? = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val response = bggService.thing2(gameId, 1)
        for (game in response.games) {
            dao.save(game.mapToEntity(), timestamp)
            Timber.i("Synced game '$gameId'")
        }
        response.games.firstOrNull()?.mapToEntity()
    }

    suspend fun refreshHeroImage(game: GameEntity): GameEntity = withContext(Dispatchers.IO) {
        val response = Adapter.createGeekdoApi().image2(game.thumbnailUrl.getImageId())
        val url = response.images.medium.url
        dao.upsert(game.id, contentValuesOf(BggContract.Games.HERO_IMAGE_URL to url))
        game.copy(heroImageUrl = url)
    }

    suspend fun getRanks(gameId: Int) = dao.loadRanks(gameId)

    suspend fun getLanguagePoll(gameId: Int) = dao.loadPoll(gameId, BggContract.POLL_TYPE_LANGUAGE_DEPENDENCE)

    suspend fun getAgePoll(gameId: Int) = dao.loadPoll(gameId, BggContract.POLL_TYPE_SUGGESTED_PLAYER_AGE)

    suspend fun getPlayerPoll(gameId: Int) = dao.loadPlayerPoll(gameId)

    suspend fun getDesigners(gameId: Int) = dao.loadDesigners(gameId)

    suspend fun getArtists(gameId: Int) = dao.loadArtists(gameId)

    suspend fun getPublishers(gameId: Int) = dao.loadPublishers(gameId)

    suspend fun getCategories(gameId: Int) = dao.loadCategories(gameId)

    suspend fun getMechanics(gameId: Int) = dao.loadMechanics(gameId)

    suspend fun getExpansions(gameId: Int) = dao.loadExpansions(gameId)

    suspend fun getBaseGames(gameId: Int) = dao.loadExpansions(gameId, true)

    suspend fun getPlaysC(gameId: Int) = playDao.loadPlaysByGameC(gameId)

    suspend fun refreshPlays(gameId: Int): List<PlayEntity> = withContext(Dispatchers.IO) {
        val plays = mutableListOf<PlayEntity>()
        val mapper = PlayMapper()
        val timestamp = System.currentTimeMillis()
        var page = 1
        do {
            val response = bggService.playsByGameC(username, gameId, page++)
            val playsPage = mapper.map(response.plays, timestamp)
            playDao.save(playsPage, timestamp)
            plays += playsPage
        } while (response.hasMorePages())

        playDao.deleteUnupdatedPlays(gameId, timestamp)
        dao.update(gameId, contentValuesOf(BggContract.Games.UPDATED_PLAYS to System.currentTimeMillis()))

        CalculatePlayStatsTask(application).executeAsyncTask()

        plays
    }

    suspend fun refreshPartialPlays(gameId: Int): List<PlayEntity> = withContext(Dispatchers.IO) {
        val mapper = PlayMapper()
        val timestamp = System.currentTimeMillis()
        val response = bggService.playsByGameC(username, gameId, 1)
        val plays = mapper.map(response.plays, timestamp)
        playDao.save(plays, timestamp)
        CalculatePlayStatsTask(application).executeAsyncTask()
        plays
    }

    fun getPlays(gameId: Int): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : RefreshableResourceLoader<List<PlayEntity>, PlaysResponse>(application) {
            var timestamp = 0L
            var isFullRefresh = false

            override val typeDescriptionResId: Int
                get() = R.string.title_plays

            override fun loadFromDatabase(): LiveData<List<PlayEntity>> {
                return playDao.loadPlaysByGame(gameId)
            }

            override fun shouldRefresh(data: List<PlayEntity>?): Boolean {
                if (isFullRefresh) return false
                if (gameId == BggContract.INVALID_ID || username.isNullOrBlank()) return false
                if (data == null) return true
                val syncTimestamp = data.firstOrNull()?.updatedPlaysTimestamp ?: 0L
                return if (syncTimestamp.isOlderThan(refreshPlaysFullHours, TimeUnit.HOURS)) {
                    isFullRefresh = true
                    true
                } else {
                    isFullRefresh = false
                    syncTimestamp.isOlderThan(refreshPlaysPartialMinutes, TimeUnit.MINUTES)
                }
            }

            override fun createCall(page: Int): Call<PlaysResponse> {
                if (page == 1) timestamp = System.currentTimeMillis()
                return bggService.playsByGame(username, gameId, page)
            }

            override fun saveCallResult(result: PlaysResponse) {
                val mapper = PlayMapper()
                val plays = mapper.map(result.plays, timestamp)
                playDao.save(plays, timestamp)
                Timber.i("Synced plays for game ID %s (page %,d)", gameId, 1)
            }

            override fun hasMorePages(result: PlaysResponse, currentPage: Int) = isFullRefresh && result.hasMorePages()

            override fun onRefreshSucceeded() {
                if (isFullRefresh) {
                    playDao.deleteUnupdatedPlays(gameId, timestamp)

                    val values = ContentValues(1)
                    values.put(BggContract.Games.UPDATED_PLAYS, System.currentTimeMillis())
                    dao.update(gameId, values)
                }
                CalculatePlayStatsTask(application).executeAsyncTask()
                isFullRefresh = false
            }

            override fun onRefreshFailed() {
                isFullRefresh = false
            }

            override fun onRefreshCancelled() {
                isFullRefresh = false
            }
        }.asLiveData()
    }

    suspend fun getPlayColors(gameId: Int) = dao.loadPlayColors(gameId)

    suspend fun getColors(gameId: Int) = dao.loadColors(gameId)

    suspend fun addPlayColor(gameId: Int, color: String?) {
        if (gameId != BggContract.INVALID_ID && !color.isNullOrBlank()) {
            dao.insertColor(gameId, color)
        }
    }

    suspend fun deletePlayColor(gameId: Int, color: String): Int {
        return if (gameId != BggContract.INVALID_ID && color.isNotBlank()) {
            dao.deleteColor(gameId, color)
        } else 0
    }

    suspend fun computePlayColors(gameId: Int) = withContext(Dispatchers.IO) {
        dao.computeColors(gameId)
    }

    suspend fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            if (gameId != BggContract.INVALID_ID) {
                dao.updateC(gameId, contentValuesOf(BggContract.Games.LAST_VIEWED to lastViewed))
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
        if (gameId != BggContract.INVALID_ID) {
            val values = contentValuesOf(
                BggContract.Games.ICON_COLOR to iconColor,
                BggContract.Games.DARK_COLOR to darkColor,
                BggContract.Games.WINS_COLOR to winsColor,
                BggContract.Games.WINNABLE_PLAYS_COLOR to winnablePlaysColor,
                BggContract.Games.ALL_PLAYS_COLOR to allPlaysColor,
            )
            val numberOfRowsModified = dao.updateC(gameId, values)
            Timber.d(numberOfRowsModified.toString())
        }
    }

    suspend fun updateFavorite(gameId: Int, isFavorite: Boolean) {
        if (gameId != BggContract.INVALID_ID) {
            dao.updateC(gameId, contentValuesOf(BggContract.Games.STARRED to if (isFavorite) 1 else 0))
        }
    }
}
