package com.boardgamegeek.service

import android.content.SyncResult
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatList
import com.boardgamegeek.repository.GameRepository
import com.boardgamegeek.util.RemoteConfig
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

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

    override fun execute() {
        Timber.i(introLogMessage)
        try {
            var numberOfFetches = 0
            do {
                if (isCancelled) break

                if (numberOfFetches > 0) if (wasSleepInterrupted(fetchPauseMillis.milliseconds)) return

                numberOfFetches++
                val gameList = runBlocking { getGames() }
                if (gameList.isNotEmpty()) {
                    val gamesDescription = gameList.map { "${it.second} [${it.first}]" }.formatList()
                    Timber.i("Found ${gameList.size} games to update [$gamesDescription]")
                    var detail = context.resources.getQuantityString(R.plurals.sync_notification_games, gameList.size, gameList.size, gamesDescription)
                    if (numberOfFetches > 1) {
                        detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches)
                    }
                    updateProgressNotification(detail)

                    val gameIds = gameList.map { it.first }
                    try {
                        val c = runBlocking { gameRepository.refreshGame(*gameIds.toIntArray()) }
                        syncResult.stats.numUpdates += c
                        if (c > 0) {
                            Timber.i("Saved %,d records for games=[$gamesDescription]", c)
                        }else{
                            Timber.i("No games saved; aborting")
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
            Timber.i("Syncing games completed successfully")
        } catch (e: Exception) {
            Timber.i("Syncing games ended with exception:\n$e")
        }
    }

    private fun syncGame(id: Int, gameName: String): Boolean {
        var detail = ""
        try {
            detail = context.resources.getQuantityString(R.plurals.sync_notification_games, 1, 1, gameName)
            runBlocking {
                val count = gameRepository.refreshGame(id)
                syncResult.stats.numUpdates += count
                Timber.i("Saved %,d records for game ID=[$id]", count)
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
