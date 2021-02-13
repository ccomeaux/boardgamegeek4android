package com.boardgamegeek.tasks.sync

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.contentValuesOf
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.extensions.whereZeroOrNull
import com.boardgamegeek.io.model.PlaysResponse
import com.boardgamegeek.mappers.PlayMapper
import com.boardgamegeek.model.persister.PlayPersister
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.tasks.CalculatePlayStatsTask
import retrofit2.Call
import timber.log.Timber

class SyncPlaysByGameTask(private val application: BggApplication, private val gameId: Int) : SyncTask<PlaysResponse>(application.applicationContext) {
    private val username = AccountUtils.getUsername(context)
    private val persister = PlayPersister(context)
    private val mapper = PlayMapper()

    @get:StringRes
    override val typeDescriptionResId: Int
        get() = R.string.title_plays

    override fun createCall(): Call<PlaysResponse>? {
        return bggService?.playsByGame(username, gameId, currentPage)
    }

    override val isRequestParamsValid: Boolean
        get() = gameId != BggContract.INVALID_ID && !username.isNullOrBlank()

    override fun persistResponse(body: PlaysResponse?) {
        body?.plays?.let {
            val plays = mapper.map(it)
            persister.save(plays, startTime)
        }
        Timber.i("Synced plays for game ID %s (page %,d)", gameId, currentPage)
    }

    override fun hasMorePages(body: PlaysResponse?): Boolean {
        return body?.hasMorePages() == true
    }

    override fun finishSync() {
        deleteUnupdatedPlays(context, startTime)
        updateGameTimestamp(context)
        CalculatePlayStatsTask(application).executeAsyncTask()
    }

    private fun deleteUnupdatedPlays(context: Context, startTime: Long) {
        val count = context.contentResolver.delete(Plays.CONTENT_URI,
                "${Plays.SYNC_TIMESTAMP}<? AND ${Plays.OBJECT_ID}=? AND ${Plays.UPDATE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DIRTY_TIMESTAMP.whereZeroOrNull()}",
                arrayOf(startTime.toString(), gameId.toString()))
        Timber.i("Deleted %,d unupdated play(s) of game ID=%s", count, gameId)
    }

    private fun updateGameTimestamp(context: Context) {
        val values = contentValuesOf(Games.UPDATED_PLAYS to System.currentTimeMillis())
        context.contentResolver.update(Games.buildGameUri(gameId), values, null, null)
    }
}