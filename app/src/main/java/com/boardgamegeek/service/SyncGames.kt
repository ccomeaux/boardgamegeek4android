package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.GameDao
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.io.BggService
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract.Games
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.IOException

abstract class SyncGames(application: BggApplication, service: BggService, syncResult: SyncResult) : SyncTask(application, service, syncResult) {
    private val dao = GameDao(application)

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
                if (gameList.isNotEmpty()) {
                    val gamesDescription = gameList.values.toList().formatList()
                    Timber.i("...found ${gameList.size} games to update [$gamesDescription]")
                    var detail = context.resources.getQuantityString(R.plurals.sync_notification_games, gameList.size, gameList.size, gamesDescription)
                    if (numberOfFetches > 1) {
                        detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches)
                    }
                    updateProgressNotification(detail)

                    val gameIds = gameList.keys.joinToString(",")
                    val call = service.thing(gameIds, 1)
                    try {
                        val timestamp = System.currentTimeMillis()
                        val response = call.execute()
                        if (response.isSuccessful) {
                            val games = response.body()?.games.orEmpty()
                            if (games.isNotEmpty()) {
                                for (game in games) {
                                    val entity = game.mapToEntity()
                                    runBlocking {
                                        if (entity.name.isBlank()) {
                                            dao.delete(entity.id)
                                        } else {
                                            dao.save(entity, timestamp)
                                        }
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
                            if (cause.message.orEmpty().startsWith("Didn't find class \"messagebox error\" on path")) {
                                Timber.i("Invalid list of game IDs: %s", gameIds)
                                for ((id, name) in gameList) {
                                    val shouldBreak = syncGame(id, name)
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
            val timestamp = System.currentTimeMillis()
            val response = call.execute()
            if (response.isSuccessful) {
                runBlocking {
                    val games = response.body()?.games.orEmpty()
                    detail = context.resources.getQuantityString(R.plurals.sync_notification_games, 1, 1, gameName)
                    for (game in games) {
                        val entity = game.mapToEntity()
                        if (entity.name.isBlank()) {
                            dao.delete(entity.id)
                        } else {
                            dao.save(entity, timestamp)
                        }
                    }
                    syncResult.stats.numUpdates += games.size.toLong()
                    Timber.i("...saved %,d games", games.size)
                }
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
                if (cause.message.orEmpty().startsWith("Didn't find class \"messagebox error\" on path")) {
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

    private fun getGames(gamesPerFetch: Int): Map<Int, String> {
        val games = mutableMapOf<Int, String>()
        val cursor = context.contentResolver.query(
            Games.CONTENT_URI,
            arrayOf(Games.Columns.GAME_ID, Games.Columns.GAME_NAME),
            selection,
            null,
            "games.${Games.Columns.UPDATED_LIST} LIMIT $gamesPerFetch"
        )
        cursor?.use {
            while (it.moveToNext()) {
                games[it.getInt(0)] = it.getString(1)
            }
        }
        return games
    }
}
