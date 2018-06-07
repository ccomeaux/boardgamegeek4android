package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.content.ContentValues
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.db.PlayDao
import com.boardgamegeek.entities.GamePlayerPollEntity
import com.boardgamegeek.entities.GamePollEntity
import com.boardgamegeek.entities.GameRankEntity
import com.boardgamegeek.entities.RefreshableResource
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.Image
import com.boardgamegeek.io.model.ThingResponse
import com.boardgamegeek.isOlderThan
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.GameMapper
import com.boardgamegeek.model.PlaysResponse
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.PlaysByGame
import com.boardgamegeek.util.ImageUtils
import com.boardgamegeek.util.RemoteConfig
import com.boardgamegeek.util.TaskUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameRepository(val application: BggApplication) {
    private var dao = GameDao(application)
    private var playDao = PlayDao(application)
    private val refreshMinutes = RemoteConfig.getInt(RemoteConfig.KEY_REFRESH_GAME_MINUTES)
    private val username: String? by lazy {
        AccountUtils.getUsername(application)
    }

    /**
     * Get a game from the database and potentially refresh it from BGG.
     */
    fun getGame(gameId: Int): LiveData<RefreshableResource<Game>> {
        val started = AtomicBoolean()
        val mediatorLiveData = MediatorLiveData<RefreshableResource<Game>>()
        val liveData = object : RefreshableResourceLoader<Game, ThingResponse>(application) {
            private var timestamp = 0L

            override val typeDescriptionResId = R.string.title_game

            override fun loadFromDatabase() = dao.load(gameId)

            override fun shouldRefresh(data: Game?): Boolean {
                if (gameId == BggContract.INVALID_ID) return false
                return data == null || data.updated.isOlderThan(refreshMinutes, TimeUnit.MINUTES)
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
            maybeRefreshHeroImageUrl(it?.data, started)
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

    fun getDesigners(gameId: Int): LiveData<List<Pair<Int, String>>> {
        return dao.loadDesigners(gameId)
    }

    fun getArtists(gameId: Int): LiveData<List<Pair<Int, String>>> {
        return dao.loadArtists(gameId)
    }

    fun getPublishers(gameId: Int): LiveData<List<Pair<Int, String>>> {
        return dao.loadPublishers(gameId)
    }

    fun getCategories(gameId: Int): LiveData<List<Pair<Int, String>>> {
        return dao.loadCategories(gameId)
    }

    fun getMechanics(gameId: Int): LiveData<List<Pair<Int, String>>> {
        return dao.loadMechanics(gameId)
    }

    fun getExpansions(gameId: Int): LiveData<List<Pair<Int, String>>> {
        return dao.loadExpansions(gameId)
    }

    fun getBaseGames(gameId: Int): LiveData<List<Pair<Int, String>>> {
        return dao.loadExpansions(gameId, true)
    }

    fun getPlays(gameId: Int): LiveData<RefreshableResource<PlaysByGame>> {
        return object : RefreshableResourceLoader<PlaysByGame, PlaysResponse>(application) {
            val persister = PlayPersister(application)
            var timestamp = 0L

            override val typeDescriptionResId: Int
                get() = R.string.title_plays

            override fun loadFromDatabase(): LiveData<PlaysByGame> {
                return dao.loadPlays(gameId)
            }

            override fun shouldRefresh(data: PlaysByGame?): Boolean {
                if (gameId == BggContract.INVALID_ID || username.isNullOrBlank()) return false
                return data == null || data.maxDate.isOlderThan(15, TimeUnit.MINUTES)
            }

            override fun createCall(page: Int): Call<PlaysResponse> {
                timestamp = System.currentTimeMillis()
                return Adapter.createForXml().playsByGame(username, gameId, page)
            }

            override fun saveCallResult(result: PlaysResponse) {
                persister.save(result.plays, timestamp)
                Timber.i("Synced plays for game ID %s (page %,d)", gameId, 1)
            }

            override fun hasMorePages(result: PlaysResponse) = result.hasMorePages()

            override fun finishSync() {
                playDao.deleteUnupdatedPlays(gameId, timestamp)

                val values = ContentValues(1)
                values.put(BggContract.Games.UPDATED_PLAYS, System.currentTimeMillis())
                dao.update(gameId, values)

                if (SyncPrefs.isPlaysSyncUpToDate(application)) {
                    TaskUtils.executeAsyncTask(CalculatePlayStatsTask(application))
                }
            }
        }.asLiveData()
    }

    fun getPlayColors(gameId: Int): LiveData<List<String>> {
        return dao.loadPlayColors(gameId)
    }

    fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = ContentValues()
            values.put(BggContract.Games.LAST_VIEWED, lastViewed)
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

    private fun maybeRefreshHeroImageUrl(game: Game?, started: AtomicBoolean) {
        if (game == null) return
        val heroImageId = ImageUtils.getImageId(game.heroImageUrl)
        val thumbnailId = ImageUtils.getImageId(game.thumbnailUrl)
        if (heroImageId != thumbnailId && started.compareAndSet(false, true)) {
            val call = Adapter.createGeekdoApi().image(thumbnailId)
            call.enqueue(object : Callback<Image> {
                override fun onResponse(call: Call<Image>?, response: Response<Image>?) {
                    if (response?.isSuccessful == true) {
                        val body = response.body()
                        if (body != null) {
                            application.appExecutors.diskIO.execute {
                                val values = ContentValues()
                                values.put(BggContract.Games.HERO_IMAGE_URL, body.images.medium.url)
                                dao.update(game.id, values)
                            }
                        } else {
                            Timber.w("Empty body while fetching image $thumbnailId for game ${game.id}")
                        }
                    } else {
                        val message = response?.message() ?: response?.code().toString()
                        Timber.w("Unsuccessful response of '$message' while fetching image $thumbnailId for game ${game.id}")
                    }
                    started.set(false)
                }

                override fun onFailure(call: Call<Image>?, t: Throwable?) {
                    val message = t?.localizedMessage ?: "Unknown error"
                    Timber.w("Unsuccessful response of '$message' while fetching image $thumbnailId for game ${game.id}")
                    started.set(false)
                }
            })
        }
    }
}
