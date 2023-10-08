package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.*
import com.boardgamegeek.model.*
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.extensions.toMillis
import com.boardgamegeek.io.model.PlayRemote
import com.boardgamegeek.io.model.PlayerRemote
import com.boardgamegeek.provider.BggContract
import okhttp3.FormBody
import java.text.SimpleDateFormat
import java.util.Locale

fun List<PlayRemote>?.mapToModel(syncTimestamp: Long = System.currentTimeMillis()) = this?.map { it.mapToModel(syncTimestamp) }.orEmpty()

private fun PlayRemote.mapToModel(syncTimestamp: Long) = Play(
    internalId = BggContract.INVALID_ID.toLong(),
    playId = id,
    dateInMillis = date.toMillis(SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US), Play.UNKNOWN_DATE),
    gameId = objectid,
    gameName = name,
    location = location,
    quantity = quantity,
    length = length,
    incomplete = incomplete == 1,
    noWinStats = nowinstats == 1,
    comments = comments.orEmpty(),
    syncTimestamp = syncTimestamp,
    initialPlayerCount = players?.size ?: 0,
    subtypes = subtypes.map { it.value },
    _players = players?.map { it.mapToModel() },
)

private fun PlayerRemote.mapToModel() = PlayPlayer(
    username = username,
    name = name,
    startingPosition = startposition,
    color = color,
    score = score,
    rating = rating,
    userId = userid,
    isNew = new_ == 1,
    isWin = win == 1,
)

@Suppress("SpellCheckingInspection")
fun Int.mapToFormBodyForDelete() = FormBody.Builder()
    .add("ajax", "1")
    .add("action", "delete")
    .add("playid", this.toString())
    .add("finalize", "1")

@Suppress("SpellCheckingInspection")
fun Play.mapToFormBodyForUpsert(): FormBody.Builder {
    val bodyBuilder = FormBody.Builder()
        .add("ajax", "1")
        .add("action", "save")
        .add("version", "2")
        .add("objecttype", "thing")
    if (this.playId > 0)
        bodyBuilder.add("playid", this.playId.toString())
    bodyBuilder.add("objectid", this.gameId.toString())
        .add("playdate", this.dateInMillis.asDateForApi())
        .add("dateinput", this.dateInMillis.asDateForApi())
        .add("length", this.length.toString())
        .add("location", this.location)
        .add("quantity", this.quantity.toString())
        .add("incomplete", if (this.incomplete) "1" else "0")
        .add("nowinstats", if (this.noWinStats) "1" else "0")
        .add("comments", this.comments)
    players.forEachIndexed { i, player ->
        bodyBuilder
            .add(createIndexedKey(i, "playerid"), "player_$i")
            .add(createIndexedKey(i, "name"), player.name)
            .add(createIndexedKey(i, "username"), player.username)
            .add(createIndexedKey(i, "color"), player.color)
            .add(createIndexedKey(i, "position"), player.startingPosition)
            .add(createIndexedKey(i, "score"), player.score)
            .add(createIndexedKey(i, "rating"), player.rating.toString())
            .add(createIndexedKey(i, "new"), if (player.isNew) "1" else "0")
            .add(createIndexedKey(i, "win"), if (player.isWin) "1" else "0")
    }
    return bodyBuilder
}

private fun createIndexedKey(index: Int, key: String) = "players[$index][$key]"

fun PlayLocal.mapToModel() = Play(
    internalId = internalId,
    playId = playId ?: BggContract.INVALID_ID,
    dateInMillis = date.toMillis(SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US), Play.UNKNOWN_DATE),
    gameId = objectId,
    gameName = itemName,
    quantity = quantity,
    length = length,
    location = location.orEmpty(),
    incomplete = incomplete,
    noWinStats = noWinStats,
    comments = comments.orEmpty(),
    syncTimestamp = syncTimestamp,
    initialPlayerCount = playerCount ?: 0,
    startTime = startTime ?: 0L,
    dirtyTimestamp = dirtyTimestamp ?: 0L,
    deleteTimestamp = deleteTimestamp ?: 0L,
    updateTimestamp = updateTimestamp ?: 0L,
    imageUrl = gameImageUrl.orEmpty(),
    thumbnailUrl = gameThumbnailUrl.orEmpty(),
    heroImageUrl = gameHeroImageUrl.orEmpty(),
    _players = players?.map { it.mapToModel() },
)

fun PlayPlayerLocal.mapToModel() = PlayPlayer(
    internalId = internalId,
    playId = internalPlayId,
    username = username.orEmpty(),
    userId = userId,
    name = name.orEmpty(),
    startingPosition = startingPosition.orEmpty(),
    color = color.orEmpty(),
    score = score.orEmpty(),
    isNew = isNew ?: false,
    rating = rating ?: 0.0,
    isWin = isWin ?: false,
)

fun PlayerLocal.mapToModel() = Player(
    name = name,
    username = username,
    playCount = playCount ?: 0,
    winCount = winCount ?: 0,
    avatarUrl = if (avatar == "N/A") "" else avatar.orEmpty(),
)

fun PlayerColorsLocal.mapToModel() = PlayerColor(
    description = playerColor,
    sortOrder = playerColorSortOrder,
)

fun LocationBasic.mapToModel() = Location(
    name = name,
    playCount = playCount,
)

fun Play.mapToEntity(syncTimestamp: Long = 0L) = PlayBasic(
    internalId = internalId,
    playId = playId,
    date = dateForDatabase(),
    objectId = gameId,
    itemName = gameName,
    quantity = quantity,
    length = length,
    location = location,
    incomplete = incomplete,
    noWinStats = noWinStats,
    comments = comments,
    syncTimestamp = syncTimestamp,
    initialPlayerCount = players.size,
    syncHashCode = null,
    dirtyTimestamp = dirtyTimestamp,
    updateTimestamp = updateTimestamp,
    deleteTimestamp = deleteTimestamp,
    startTime = startTime,
    players = players.map { it.mapToEntity() },
)

fun PlayPlayer.mapToEntity() = PlayPlayerLocal(
    internalId = internalId,
    internalPlayId = playId,
    username = username,
    userId = userId,
    name = name,
    startingPosition = startingPosition,
    color = color,
    score = score,
    isNew = isNew,
    rating = rating,
    isWin = isWin,
)

private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd"
