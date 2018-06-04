package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.GameMapper
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.service.model.GameList
import com.boardgamegeek.use
import com.boardgamegeek.util.RemoteConfig
import timber.log.Timber
import java.io.IOException
import java.util.*

abstract class SyncGames(application: BggApplication, service: BggService, syncResult: SyncResult) : SyncTask(application, service, syncResult) {

    protected open val maxFetchCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_FETCH_MAX)

    private val gamesPerFetch = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_PER_FETCH)

    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_GAMES_FETCH_PAUSE_MILLIS)

    protected abstract val exitLogMessage: String

    protected open val selection: String? = null

    override fun execute() {
        Timber.i(getIntroLogMessage(gamesPerFetch))
        try {
            var numberOfFetches = 0
            do {
                if (isCancelled) break

                if (numberOfFetches > 0) if (wasSleepInterrupted(fetchPauseMillis)) return

                numberOfFetches++
                val gameList = getGames(gamesPerFetch)
                if (gameList.size > 0) {
                    Timber.i("...found ${gameList.size} games to update [${gameList.description}]")
                    var detail = context.resources.getQuantityString(R.plurals.sync_notification_games, gameList.size, gameList.size, gameList.description)
                    if (numberOfFetches > 1) {
                        detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches)
                    }
                    updateProgressNotification(detail)

                    val call = service.thing(gameList.ids, 1)
                    try {
                        val response = call.execute()
                        if (response.isSuccessful) {
                            val body = response.body()
                            val games = body?.games ?: emptyList()
                            if (games.isNotEmpty()) {
                                val dao = GameDao(application)
                                for (game in games) {
                                    val entity = GameMapper().map(game)
                                    if (entity.name.isBlank()) {
                                        dao.delete(entity.id)
                                    } else {
                                        dao.save(entity)
                                    }
                                }
                                syncResult.stats.numUpdates += games.size.toLong()
                                Timber.i("...saved %,d games", games.size)
                            } else {
                                Timber.i("...no games returned")
                                break
                            }
                        } else {
                            showError(detail, response.code())
                            syncResult.stats.numIoExceptions++
                            cancel()
                            return
                        }
                    } catch (e: IOException) {
                        showError(detail, e)
                        syncResult.stats.numIoExceptions++
                        break
                    } catch (e: RuntimeException) {
                        val cause = e.cause
                        if (cause is ClassNotFoundException) {
                            val message = cause.message ?: ""
                            if (message.startsWith("Didn't find class \"messagebox error\" on path")) {
                                Timber.i("Invalid list of game IDs: %s", gameList.ids)
                                for (i in 0 until gameList.size) {
                                    val shouldBreak = syncGame(gameList.getId(i), gameList.getName(i))
                                    if (shouldBreak) break
                                }
                            } else {
                                showError(detail, e)
                                syncResult.stats.numParseExceptions++
                                break
                            }
                        } else {
                            showError(detail, e)
                            syncResult.stats.numParseExceptions++
                            break
                        }
                    }
                } else {
                    Timber.i(exitLogMessage)
                    break
                }
            } while (numberOfFetches < maxFetchCount)
        } finally {
            Timber.i("...complete!")
        }
    }

    private fun syncGame(id: Int, gameName: String): Boolean {
        var detail = ""
        val call = service.thing(id, 1)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val games = if (response.body() == null) ArrayList(0) else response.body()!!.games
                detail = context.resources.getQuantityString(R.plurals.sync_notification_games, 1, 1, gameName)
                val dao = GameDao(application)
                for (game in games) {
                    val entity = GameMapper().map(game)
                    if (entity.name.isBlank()) {
                        dao.delete(entity.id)
                    } else {
                        dao.save(entity)
                    }
                }
                syncResult.stats.numUpdates += games.size.toLong()
                Timber.i("...saved %,d games", games.size)
            } else {
                showError(detail, response.code())
                syncResult.stats.numIoExceptions++
                cancel()
                return true
            }
        } catch (e: IOException) {
            showError(detail, e)
            syncResult.stats.numIoExceptions++
            return true
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause is ClassNotFoundException) {
                val message = cause.message ?: ""
                if (message.startsWith("Didn't find class \"messagebox error\" on path")) {
                    Timber.i("Invalid game $gameName ($id)")
                    showError(detail, e)
                    // otherwise just ignore this error
                } else {
                    showError(detail, e)
                    syncResult.stats.numParseExceptions++
                }
            } else {
                showError(detail, e)
                syncResult.stats.numParseExceptions++
            }
            return false
        }

        return false
    }

    protected abstract fun getIntroLogMessage(gamesPerFetch: Int): String

    private fun getGames(gamesPerFetch: Int): GameList {
        val list = GameList(gamesPerFetch)
        val cursor = context.contentResolver.query(Games.CONTENT_URI,
                arrayOf(Games.GAME_ID, Games.GAME_NAME),
                selection,
                null,
                "games.${Games.UPDATED_LIST} LIMIT $gamesPerFetch")
        cursor?.use {
            while (it.moveToNext()) {
                list.addGame(it.getInt(0), it.getString(1))
            }
        }
        return list
    }
}
