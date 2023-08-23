package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.LocationBasic
import com.boardgamegeek.db.model.PlayerColorsLocal
import com.boardgamegeek.db.model.PlayerLocal
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.asDateForApi
import com.boardgamegeek.io.model.Play
import com.boardgamegeek.io.model.Player
import com.boardgamegeek.provider.BggContract
import okhttp3.FormBody

fun List<Play>?.mapToEntity(syncTimestamp: Long = System.currentTimeMillis()) = this?.map { it.mapToEntity(syncTimestamp) }.orEmpty()

private fun Play.mapToEntity(syncTimestamp: Long) = PlayEntity(
    internalId = BggContract.INVALID_ID.toLong(),
    playId = id,
    rawDate = date,
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
    _players = players?.map { it.mapToEntity() },
)

private fun Player.mapToEntity() = PlayPlayerEntity(
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
fun PlayEntity.mapToFormBodyForUpsert(): FormBody.Builder {
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

fun PlayerLocal.mapToEntity() = PlayerEntity(
    name = name,
    username = username,
    playCount = playCount ?: 0,
    winCount = winCount ?: 0,
    avatarUrl = if (avatar == "N/A") "" else avatar.orEmpty(),
)

fun PlayerColorsLocal.mapToEntity() = PlayerColorEntity(
    description = playerColor,
    sortOrder = playerColorSortOrder,
)

fun LocationBasic.mapToEntity() = LocationEntity(
    name = name,
    playCount = playCount,
)
