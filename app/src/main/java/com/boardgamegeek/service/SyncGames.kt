package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.io.Adapter
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

abstract class SyncGames(
    application: BggApplication,
    syncResult: SyncResult,
    private val gameRepository: GameRepository,
) : SyncTask(application, syncResult) {

    private val fetchPauseMillis = RemoteConfig.getLong(RemoteConfig.KEY_SYNC_GAMES_FETCH_PAUSE_MILLIS)
    protected val gamesPerFetch = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_PER_FETCH)
    protected open val maxFetchCount = RemoteConfig.getInt(RemoteConfig.KEY_SYNC_GAMES_FETCH_MAX)

    protected abstract suspend fun getGames(): List<Pair<Int, String>>
    protected abstract val introLogMessage: String
    protected abstract val exitLogMessage: String

    private val service = Adapter.createForXmlWithAuth(application)

    override fun execute() {
        Timber.i(introLogMessage)
        try {
            var numberOfFetches = 0
            do {
                if (isCancelled) break

                if (numberOfFetches > 0) if (wasSleepInterrupted(fetchPauseMillis)) return

                numberOfFetches++
                val gameList = runBlocking { getGames() }
                if (gameList.isNotEmpty()) {
                    val gamesDescription = gameList.map { "[${it.first}] ${it.second}" }.formatList()
                    Timber.i("...found ${gameList.size} games to update [$gamesDescription]")
                    var detail =
                        context.resources.getQuantityString(R.plurals.sync_notification_games, gameList.size, gameList.size, gamesDescription)
                    if (numberOfFetches > 1) {
                        detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches)
                    }
                    updateProgressNotification(detail)

                    val gameIds = gameList.map { it.first }.joinToString(",")
                    try {
                        val timestamp = System.currentTimeMillis()
                        val response = runBlocking { service.things(gameIds, 1) }
                        val games = response.games.orEmpty()
                        if (games.isNotEmpty()) {
                            runBlocking {
                                for (game in games) {
                                    gameRepository.saveGame(game.mapToEntity(), timestamp)
                                }
                            }
                            syncResult.stats.numUpdates += games.size.toLong()
                            Timber.i("...saved %,d games", games.size)
                        } else {
                            Timber.i("...no games returned")
                            break
                        }
                    } catch (e: IOException) {
                        showError(detail, e)
                        syncResult.stats.numIoExceptions++
                        break
                    } catch (e: RuntimeException) {
                        if (e is HttpException) {
                            showError(detail, e.code())
                        } else {
                            val cause = e.cause
                            if (cause is ClassNotFoundException &&
                                cause.message.orEmpty().startsWith("Didn't find class \"messagebox error\" on path")
                            ) {
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
                        }
                    } catch (e: Exception) {
                        showError(detail, e)
                        syncResult.stats.numIoExceptions++
                        cancel()
                        return
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
        try {
            val timestamp = System.currentTimeMillis()
            runBlocking {
                val response = service.thing(id, 1)
                val games = response.games.orEmpty()
                detail = context.resources.getQuantityString(R.plurals.sync_notification_games, 1, 1, gameName)
                for (game in games) {
                    gameRepository.saveGame(game.mapToEntity(), timestamp)
                }
                syncResult.stats.numUpdates += games.size.toLong()
                Timber.i("...saved %,d games", games.size)
            }
        } catch (e: IOException) {
            showError(detail, e)
            syncResult.stats.numIoExceptions++
            return true
        } catch (e: RuntimeException) {
            if (e is HttpException) {
                showError(detail, e.code())
            } else {
                val cause = e.cause
                if (cause is ClassNotFoundException &&
                    cause.message.orEmpty().startsWith("Didn't find class \"messagebox error\" on path")
                ) {
                    Timber.i("Invalid game $gameName ($id)")
                    showError(detail, e)
                    // otherwise just ignore this error
                } else {
                    showError(detail, e)
                    syncResult.stats.numParseExceptions++
                }
                return false
            }
        } catch (e: Exception) {
            showError(detail, e)
            syncResult.stats.numIoExceptions++
            cancel()
            return true
        }

        return false
    }
}
