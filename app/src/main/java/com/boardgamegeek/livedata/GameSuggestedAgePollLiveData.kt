package com.boardgamegeek.livedata

import android.content.Context
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.entities.GameSuggestedAgePollEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.model.GameCollectionItem

class GameSuggestedAgePollLiveData(context: Context, private val gameId: Int) :
        ContentObservableLiveData<GameSuggestedAgePollEntity>(context) {
    private var dao = GameDao(context)

    override var uri = GameCollectionItem.uri

    override fun loadData() {
        postValue(GameSuggestedAgePollEntity(dao.loadPoll(gameId, BggContract.POLL_TYPE_SUGGESTED_PLAYER_AGE)))
    }
}
