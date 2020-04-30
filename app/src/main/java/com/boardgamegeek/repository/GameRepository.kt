package com.boardgamegeek.repository

import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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
import com.boardgamegeek.io.model.ThingResponse
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.GameMapper
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.util.RemoteConfig
import retrofit2.Call
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameRepository(val application: BggApplication) {
    private val dao = GameDao(application)
    private val playDao = PlayDao(application)
    private val refreshGameMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_MINUTES)
    private val refreshPlaysPartialMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_PARTIAL_MINUTES)
    private val refreshPlaysFullHours = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_PLAYS_FULL_HOURS)
    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    /**
     * Get a game from the database and potentially refresh it from BGG.
     */
    fun getGame(gameId: Int): LiveData<RefreshableResource<GameEntity>> {
        val started = AtomicBoolean()
        val mediatorLiveData = MediatorLiveData<RefreshableResource<GameEntity>>()
        val liveData = object : RefreshableResourceLoader<GameEntity, ThingResponse>(application) {
            private var timestamp = 0L

            override val typeDescriptionResId = R.string.title_game

            override fun loadFromDatabase() = dao.load(gameId)

            override fun shouldRefresh(data: GameEntity?): Boolean {
                if (gameId == BggContract.INVALID_ID) return false
                return data == null || data.updated.isOlderThan(refreshGameMinutes, TimeUnit.MINUTES)
            }

            override fun createCall(page: Int): Call<ThingResponse> {
                timestamp = System.currentTimeMillis()
                return Adapter.createForXml().thing(gameId, 1)
            }

            override fun saveCallResult(result: ThingResponse) {
                for (game in result.games) {
                    dao.save(GameMapper().map(game), timestamp)
                    Timber.i("Synced game '$gameId'")
                }
            }
        }.asLiveData()
        mediatorLiveData.addSource(liveData) {
            it?.data?.maybeRefreshHeroImageUrl("game", started) { url ->
                application.appExecutors.diskIO.execute {
                    dao.update(gameId, ContentValues().apply {
                        put(BggContract.Games.HERO_IMAGE_URL, url)
                    })
                }
            }
            mediatorLiveData.value = it
        }
        return mediatorLiveData
    }

    fun getLanguagePoll(gameId: Int): LiveData<GamePollEntity> {
        return dao.loadPoll(gameId, BggContract.POLL_TYPE_LANGUAGE_DEPENDENCE)
    }

    fun getAgePoll(gameId: Int): LiveData<GamePollEntity> {
        return dao.loadPoll(gameId, BggContract.POLL_TYPE_SUGGESTED_PLAYER_AGE)
    }

    fun getRanks(gameId: Int): LiveData<List<GameRankEntity>> {
        return dao.loadRanks(gameId)
    }

    fun getPlayerPoll(gameId: Int): LiveData<GamePlayerPollEntity> {
        return dao.loadPlayerPoll(gameId)
    }

    fun getDesigners(gameId: Int) = dao.loadDesigners(gameId)

    fun getArtists(gameId: Int) = dao.loadArtists(gameId)

    fun getPublishers(gameId: Int) = dao.loadPublishers(gameId)

    fun getCategories(gameId: Int) = dao.loadCategories(gameId)

    fun getMechanics(gameId: Int) = dao.loadMechanics(gameId)

    fun getExpansions(gameId: Int) = dao.loadExpansions(gameId)

    fun getBaseGames(gameId: Int) = dao.loadExpansions(gameId, true)

    fun getPlays(gameId: Int): LiveData<RefreshableResource<List<PlayEntity>>> {
        return object : RefreshableResourceLoader<List<PlayEntity>, PlaysResponse>(application) {
            val persister = PlayPersister(application)
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
                return Adapter.createForXml().playsByGame(username, gameId, page)
            }

            override fun saveCallResult(result: PlaysResponse) {
                val mapper = PlayMapper()
                val plays = mapper.map(result.plays)
                persister.save(plays, timestamp)
                Timber.i("Synced plays for game ID %s (page %,d)", gameId, 1)
            }

            override fun hasMorePages(result: PlaysResponse) = isFullRefresh && result.hasMorePages()

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

    fun getPlayColors(gameId: Int): LiveData<List<String>> {
        return dao.loadPlayColors(gameId)
    }

    fun addPlayColor(gameId: Int, color: String) {
        if (gameId == BggContract.INVALID_ID) return
        if (color.isBlank()) return
        application.appExecutors.diskIO.execute {
            dao.insertColors(gameId, color)
        }
    }

    fun deletePlayColor(gameId: Int, color: String): Int {
        if (gameId == BggContract.INVALID_ID) return 0
        if (color.isBlank()) return 0
        application.appExecutors.diskIO.execute {
            dao.deleteColor(gameId, color)
        }
        return 1
    }

    fun computePlayColors(gameId: Int) {
        application.appExecutors.diskIO.execute {
            dao.computeColors(gameId)
        }
    }

    fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = contentValuesOf(BggContract.Games.LAST_VIEWED to lastViewed)
            dao.update(gameId, values)
        }
    }

    fun updateGameColors(gameId: Int, iconColor: Int, darkColor: Int, winsColor: Int, winnablePlaysColor: Int, allPlaysColor: Int) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = ContentValues(5)
            values.put(BggContract.Games.ICON_COLOR, iconColor)
            values.put(BggContract.Games.DARK_COLOR, darkColor)
            values.put(BggContract.Games.WINS_COLOR, winsColor)
            values.put(BggContract.Games.WINNABLE_PLAYS_COLOR, winnablePlaysColor)
            values.put(BggContract.Games.ALL_PLAYS_COLOR, allPlaysColor)
            val numberOfRowsModified = dao.update(gameId, values)
            Timber.d(numberOfRowsModified.toString())
        }
    }

    fun updateFavorite(gameId: Int, isFavorite: Boolean) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = ContentValues()
            values.put(BggContract.Games.STARRED, if (isFavorite) 1 else 0)
            dao.update(gameId, values)
        }
    }
}
