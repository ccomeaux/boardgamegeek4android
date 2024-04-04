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

fun PlayEntity.mapToModel() = Play(
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
)

fun PlayWithPlayersEntity.mapToModel() = Play(
    internalId = play.internalId,
    playId = play.playId ?: BggContract.INVALID_ID,
    dateInMillis = play.date.toMillis(SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US), Play.UNKNOWN_DATE),
    gameId = play.objectId,
    gameName = play.itemName,
    quantity = play.quantity,
    length = play.length,
    location = play.location.orEmpty(),
    incomplete = play.incomplete,
    noWinStats = play.noWinStats,
    comments = play.comments.orEmpty(),
    syncTimestamp = play.syncTimestamp,
    initialPlayerCount = play.playerCount ?: 0,
    startTime = play.startTime ?: 0L,
    dirtyTimestamp = play.dirtyTimestamp ?: 0L,
    deleteTimestamp = play.deleteTimestamp ?: 0L,
    updateTimestamp = play.updateTimestamp ?: 0L,
    _players = players.map { it.mapToModel() }.sortedBy { it.seat },
)

fun PlayWithPlayersAndImagesEntity.mapToModel() = Play(
    internalId = play.internalId,
    playId = play.playId ?: BggContract.INVALID_ID,
    dateInMillis = play.date.toMillis(SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US), Play.UNKNOWN_DATE),
    gameId = play.objectId,
    gameName = play.itemName,
    quantity = play.quantity,
    length = play.length,
    location = play.location.orEmpty(),
    incomplete = play.incomplete,
    noWinStats = play.noWinStats,
    comments = play.comments.orEmpty(),
    syncTimestamp = play.syncTimestamp,
    initialPlayerCount = play.playerCount ?: 0,
    startTime = play.startTime ?: 0L,
    dirtyTimestamp = play.dirtyTimestamp ?: 0L,
    deleteTimestamp = play.deleteTimestamp ?: 0L,
    updateTimestamp = play.updateTimestamp ?: 0L,
    imageUrl = gameImageUrl.orEmpty(),
    thumbnailUrl = gameThumbnailUrl.orEmpty(),
    heroImageUrl = gameHeroImageUrl.orEmpty(),
    _players = players.map { it.mapToModel() }.sortedBy { it.seat },
)

fun PlayPlayerEntity.mapToModel() = PlayPlayer(
    internalId = internalId,
    playInternalId = internalPlayId,
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

fun List<PlayerWithPlayEntity>.mapToModel() =
    firstOrNull()?.let {
        if (!it.player.name.isNullOrBlank() || !it.player.username.isNullOrBlank()) {
            Player(
                it.player.name.orEmpty(),
                it.player.username.orEmpty(),
                sumOf { play -> play.quantity },
                filter { play -> !play.noWinStats && play.player.isWin == true }.sumOf { play -> play.quantity },
                it.avatarUrl.takeIf { url ->  url != "N/A" }.orEmpty(),
            )
        } else null
    }

fun PlayerColorsEntity.mapToModel() = PlayerColor(
    description = playerColor,
    sortOrder = playerColorSortOrder,
)

fun LocationEntity.mapToModel() = Location(
    name = name,
    playCount = playCount,
)

fun Play.mapToEntity(syncTimestamp: Long) = PlayEntity(
    internalId = if (internalId == BggContract.INVALID_ID.toLong()) 0 else internalId,
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
    playerCount = players.size,
    syncHashCode = generateSyncHashCode(),
    dirtyTimestamp = dirtyTimestamp,
    updateTimestamp = updateTimestamp,
    deleteTimestamp = deleteTimestamp,
    startTime = startTime,
)

fun PlayPlayer.mapToEntity() = PlayPlayerEntity(
    internalId = if (internalId == BggContract.INVALID_ID.toLong()) 0 else internalId,
    internalPlayId = playInternalId,
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
