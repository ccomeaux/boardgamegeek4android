package com.boardgamegeek.repository

import android.arch.lifecycle.LiveData
import android.content.ContentValues
import android.content.Context
import android.os.Handler
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.livedata.GameLiveData
import com.boardgamegeek.livedata.RefreshableResourceLoader
import com.boardgamegeek.mappers.GameMapper
import com.boardgamegeek.model.ThingResponse
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
        Handler().post({
            val values = ContentValues()
            values.put(BggContract.Games.LAST_VIEWED, lastViewed)
            application.contentResolver.update(BggContract.Games.buildGameUri(gameId), values, null, null)
        })
    }

    inner class GameLoader(context: Context) : RefreshableResourceLoader<Game, ThingResponse>(context) {
        override val typeDescriptionResId = R.string.title_game

        override fun isRequestParamsValid() = gameId != BggContract.INVALID_ID

        override fun loadFromDatabase() = GameLiveData(context, gameId)

        override fun shouldRefresh(data: Game?): Boolean {
            return data == null ||
                    DateTimeUtils.howManyDaysOld(data.updated) > AGE_IN_DAYS_TO_REFRESH ||
                    data.pollsVoteCount == 0
        }

        override fun createCall(): Call<ThingResponse> = Adapter.createForXml().thing(gameId, 1)

        override fun saveCallResult(item: ThingResponse) {
            val dao = GameDao(application)
            for (game in item.games) {
                dao.save(GameMapper().map(game))
                Timber.i("Synced game '$gameId'")
            }
        }
    }
}
