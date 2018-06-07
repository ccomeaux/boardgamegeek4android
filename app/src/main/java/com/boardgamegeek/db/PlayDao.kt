package com.boardgamegeek.db

import com.boardgamegeek.BggApplication
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.whereZeroOrNull
import timber.log.Timber

class PlayDao(private val context: BggApplication) {
    fun deleteUnupdatedPlays(gameId: Int, since: Long) {
        val count = context.contentResolver.delete(Plays.CONTENT_URI,
                "${Plays.SYNC_TIMESTAMP}<? AND ${Plays.OBJECT_ID}=? AND ${Plays.UPDATE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DIRTY_TIMESTAMP.whereZeroOrNull()}",
                arrayOf(since.toString(), gameId.toString()))
        Timber.i("Deleted %,d unupdated play(s) of game ID=%s", count, gameId)
    }
}

