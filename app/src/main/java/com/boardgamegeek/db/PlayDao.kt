package com.boardgamegeek.db

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.auth.AccountUtils
import com.boardgamegeek.entities.PlayEntity
import com.boardgamegeek.entities.PlayerEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.PlayPlayers
import com.boardgamegeek.provider.BggContract.Plays
import timber.log.Timber

class PlayDao(private val context: BggApplication) {
    fun deleteUnupdatedPlays(gameId: Int, since: Long) {
        val count = context.contentResolver.delete(Plays.CONTENT_URI,
                "${Plays.SYNC_TIMESTAMP}<? AND ${Plays.OBJECT_ID}=? AND ${Plays.UPDATE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()} AND ${Plays.DIRTY_TIMESTAMP.whereZeroOrNull()}",
                arrayOf(since.toString(), gameId.toString()))
        Timber.i("Deleted %,d unupdated play(s) of game ID=%s", count, gameId)
    }

    fun load(gameId: Int): LiveData<List<PlayEntity>> {
        if (gameId == BggContract.INVALID_ID) return AbsentLiveData.create()
        val uri = Plays.CONTENT_URI
        return RegisteredLiveData(context, uri, true) {
            val list = arrayListOf<PlayEntity>()
            context.contentResolver.load(uri,
                    arrayOf(Plays._ID,
                            Plays.PLAY_ID,
                            Plays.DATE,
                            Plays.OBJECT_ID,
                            Plays.ITEM_NAME,
                            Plays.QUANTITY,
                            Plays.LENGTH,
                            Plays.LOCATION,
                            Plays.INCOMPLETE,
                            Plays.NO_WIN_STATS,
                            Plays.COMMENTS,
                            Plays.SYNC_TIMESTAMP,
                            Plays.PLAYER_COUNT,
                            Plays.DIRTY_TIMESTAMP,
                            Plays.START_TIME),
                    "${Plays.OBJECT_ID}=? AND ${Plays.DELETE_TIMESTAMP.whereZeroOrNull()}",
                    arrayOf(gameId.toString())
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        list.add(PlayEntity(
                                internalId = it.getLong(Plays._ID),
                                playId = it.getInt(Plays.PLAY_ID),
                                date = it.getString(Plays.DATE),
                                gameId = it.getInt(Plays.OBJECT_ID),
                                gameName = it.getString(Plays.ITEM_NAME),
                                quantity = it.getIntOrNull(Plays.QUANTITY) ?: 1,
                                length = it.getIntOrNull(Plays.LENGTH) ?: 0,
                                location = it.getStringOrEmpty(Plays.LOCATION),
                                incomplete = it.getInt(Plays.INCOMPLETE) == 1,
                                noWinStats = it.getInt(Plays.NO_WIN_STATS) == 1,
                                comments = it.getStringOrEmpty(Plays.COMMENTS),
                                syncTimestamp = it.getLong(Plays.SYNC_TIMESTAMP),
                                playerCount = it.getInt(Plays.PLAYER_COUNT),
                                dirtyTimestamp = it.getLong(Plays.DIRTY_TIMESTAMP),
                                startTime = it.getLong(Plays.START_TIME)
                        ))
                    } while (it.moveToNext())
                }
            }
            return@RegisteredLiveData list
        }
    }

    fun loadPlayers(includeIncompletePlays: Boolean): LiveData<List<PlayerEntity>> {
        val uri = Plays.buildPlayersByUniquePlayerUri()
        return RegisteredLiveData(context, uri, true) {
            val selection = arrayListOf<String>().apply {
                add(Plays.DELETE_TIMESTAMP.whereZeroOrNull())
                if (!AccountUtils.getUsername(context).isNullOrBlank()) {
                    add(BggContract.PlayPlayers.USER_NAME + "!=?")
                }
                if (!includeIncompletePlays) {
                    add(Plays.INCOMPLETE.whereZeroOrNull())
                }
            }.joinTo(" AND ").toString()
            val selectionArgs = AccountUtils.getUsername(context)?.let { username ->
                when {
                    username.isBlank() -> null
                    else -> arrayOf(username)
                }
            }
            val results = arrayListOf<PlayerEntity>()
            context.contentResolver.load(uri,
                    arrayOf(
                            PlayPlayers._ID,
                            PlayPlayers.NAME,
                            PlayPlayers.USER_NAME,
                            PlayPlayers.SUM_QUANTITY,
                            PlayPlayers.SUM_WINS),
                    selection,
                    selectionArgs,
                    "${PlayPlayers.SUM_QUANTITY} DESC, ${PlayPlayers.NAME}${BggContract.COLLATE_NOCASE}"
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results += PlayerEntity(
                                it.getInt(PlayPlayers._ID),
                                it.getStringOrEmpty(PlayPlayers.NAME),
                                it.getStringOrEmpty(PlayPlayers.USER_NAME),
                                it.getIntOrZero(PlayPlayers.SUM_QUANTITY),
                                it.getIntOrZero(PlayPlayers.SUM_WINS)
                        )
                    } while (it.moveToNext())
                }
            }
            return@RegisteredLiveData results
        }
    }
}
