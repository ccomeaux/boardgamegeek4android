package com.boardgamegeek.livedata

import android.content.Context
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.entities.GameRank
import com.boardgamegeek.provider.BggContract

class GameRankLiveData(context: Context, val gameId: Int) : ContentObservableLiveData<List<GameRank>>(context) {
    private var dao = GameDao(context)

    override var uri = BggContract.Games.buildRanksUri(gameId)

    override fun loadData() {
        postValue(dao.loadRanks(gameId))
    }
}
