package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.content.ContentValues
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.io.model.ThingResponse
import com.boardgamegeek.livedata.GameLiveData
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.GameMapper
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.ui.model.RefreshableResource
import com.boardgamegeek.util.DateTimeUtils
import retrofit2.Call
import timber.log.Timber

private const val AGE_IN_DAYS_TO_REFRESH = 3

class GameRepository(val application: BggApplication) {
    private var loader: GameLoader = GameLoader(application)
    private var gameId: Int = BggContract.INVALID_ID

    /**
     * Get a game from the database and potentially refresh it from BGG.
     */
    fun getGame(gameId: Int): LiveData<RefreshableResource<Game>> {
        this.gameId = gameId
        return loader.load()
    }

    /**
     * Refresh the currently loaded game from BGG.
     */
    fun refreshGame() {
        loader.refresh()
    }

    fun updateLastViewed(lastViewed: Long = System.currentTimeMillis()) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = ContentValues()
            values.put(BggContract.Games.LAST_VIEWED, lastViewed)
            application.contentResolver.update(BggContract.Games.buildGameUri(gameId), values, null, null)
        }
    }

    fun updateHeroImageUrl(url: String, imageUrl: String, thumbnailUrl: String, heroImageUrl: String) {
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

    fun updateColors(iconColor: Int, darkColor: Int, winsColor: Int, winnablePlaysColor: Int, allPlaysColor: Int) {
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

    fun updateFavorite(isFavorite: Boolean) {
        if (gameId == BggContract.INVALID_ID) return
        application.appExecutors.diskIO.execute {
            val values = ContentValues()
            values.put(BggContract.Games.STARRED, if (isFavorite) 1 else 0)
            application.contentResolver.update(BggContract.Games.buildGameUri(gameId), values, null, null)
        }
    }

    inner class GameLoader(application: BggApplication) : RefreshableResourceLoader<Game, ThingResponse>(application) {
        override val typeDescriptionResId = R.string.title_game

        override fun isRequestParamsValid() = gameId != BggContract.INVALID_ID

        override fun loadFromDatabase() = GameLiveData(application, gameId)

        override fun shouldRefresh(data: Game?): Boolean {
            return data == null ||
                    DateTimeUtils.howManyDaysOld(data.updated) > AGE_IN_DAYS_TO_REFRESH ||
                    data.pollsVoteCount == 0
        }

        override fun createCall(): Call<ThingResponse> = Adapter.createForXml().thing(gameId, 1)

        override fun saveCallResult(result: ThingResponse) {
            val dao = GameDao(application)
            for (game in result.games) {
                dao.save(GameMapper().map(game))
                Timber.i("Synced game '$gameId'")
            }
        }
    }
}
