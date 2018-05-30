package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.content.ContentValues
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.entities.GamePollEntity
import com.boardgamegeek.entities.GameRankEntity
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.ThingResponse
import com.boardgamegeek.livedata.DatabaseResourceLoader
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.GameMapper
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.RefreshableResource
import com.boardgamegeek.util.DateTimeUtils
import retrofit2.Call
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GameRepository(val application: BggApplication) {
    private var dao = GameDao(application)

    /**
     * Get a game from the database and potentially refresh it from BGG.
     */
    fun getGame(gameId: Int): LiveData<RefreshableResource<Game>> {
        return object : RefreshableResourceLoader<Game, ThingResponse>(application) {
            override val typeDescriptionResId = R.string.title_game

            override fun loadFromDatabase() = GameDao(application).load(gameId)

            override fun shouldRefresh(data: Game?): Boolean {
                if (gameId == BggContract.INVALID_ID) return false
                return data == null || data.pollsVoteCount == 0 ||
                        DateTimeUtils.isOlderThan(data.updated, 1, TimeUnit.MINUTES)
            }

            override fun createCall(): Call<ThingResponse> = Adapter.createForXml().thing(gameId, 1)

            override fun saveCallResult(result: ThingResponse) {
                val dao = GameDao(application)
                for (game in result.games) {
                    dao.save(GameMapper().map(game))
                    Timber.i("Synced game '$gameId'")
                }
            }
        }.asLiveData()
    }

    fun getLanguagePoll(gameId: Int): LiveData<GamePollEntity> {
        return object : DatabaseResourceLoader<GamePollEntity>(application) {
            override fun loadFromDatabase(): LiveData<GamePollEntity> {
                return dao.loadPoll(gameId, BggContract.POLL_TYPE_LANGUAGE_DEPENDENCE)
            }
        }.asLiveData()
    }

    fun getAgePoll(gameId: Int): LiveData<GamePollEntity> {
        return object : DatabaseResourceLoader<GamePollEntity>(application) {
            override fun loadFromDatabase(): LiveData<GamePollEntity> {
                return dao.loadPoll(gameId, BggContract.POLL_TYPE_SUGGESTED_PLAYER_AGE)
            }
        }.asLiveData()
    }

    fun getRanks(gameId: Int): LiveData<List<GameRankEntity>> {
        return object : DatabaseResourceLoader<List<GameRankEntity>>(application) {
            override fun loadFromDatabase(): LiveData<List<GameRankEntity>> {
                return dao.loadRanks(gameId)
            }
        }.asLiveData()
    }

    fun updateLastViewed(gameId: Int, lastViewed: Long = System.currentTimeMillis()) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = ContentValues()
            values.put(BggContract.Games.LAST_VIEWED, lastViewed)
            application.contentResolver.update(BggContract.Games.buildGameUri(gameId), values, null, null)
        }
    }

    fun updateHeroImageUrl(gameId: Int, url: String, imageUrl: String, thumbnailUrl: String, heroImageUrl: String) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            if (url.isNotBlank() &&
                    url != imageUrl &&
                    url != thumbnailUrl &&
                    url != heroImageUrl) {
                val values = ContentValues()
                values.put(BggContract.Games.HERO_IMAGE_URL, url)
                application.contentResolver.update(BggContract.Games.buildGameUri(gameId), values, null, null)
            }
        }
    }

    fun updateColors(gameId: Int, iconColor: Int, darkColor: Int, winsColor: Int, winnablePlaysColor: Int, allPlaysColor: Int) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = ContentValues(5)
            values.put(BggContract.Games.ICON_COLOR, iconColor)
            values.put(BggContract.Games.DARK_COLOR, darkColor)
            values.put(BggContract.Games.WINS_COLOR, winsColor)
            values.put(BggContract.Games.WINNABLE_PLAYS_COLOR, winnablePlaysColor)
            values.put(BggContract.Games.ALL_PLAYS_COLOR, allPlaysColor)
            val numberOfRowsModified = application.contentResolver.update(BggContract.Games.buildGameUri(gameId), values, null, null)
            Timber.d(numberOfRowsModified.toString())
        }
    }

    fun updateFavorite(gameId: Int, isFavorite: Boolean) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = ContentValues()
            values.put(BggContract.Games.STARRED, if (isFavorite) 1 else 0)
            application.contentResolver.update(BggContract.Games.buildGameUri(gameId), values, null, null)
        }
    }
}
