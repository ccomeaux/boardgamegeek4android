package com.boardgamegeek.livedata

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.model.ThingResponse
import com.boardgamegeek.model.persister.GamePersister
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.Game
import com.boardgamegeek.util.DateTimeUtils
import retrofit2.Call
import timber.log.Timber

private const val AGE_IN_DAYS_TO_REFRESH = 3

class GameLoader(context: Context, private val gameId: Int) : RefreshableResourceLoader<Game, ThingResponse>(context) {
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
        val rowCount = GamePersister(context).save(item.games, "Game $gameId")
        Timber.i("Synced game '%s' (%,d rows)", gameId, rowCount)
    }
}