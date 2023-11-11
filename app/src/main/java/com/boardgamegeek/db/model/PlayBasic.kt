package com.boardgamegeek.db.model

data class PlayBasic(
    val internalId: Long,
    val playId: Int?,
    val date: String,
    val objectId: Int,
    val itemName: String,
    val quantity: Int,
    val length: Int,
    val location: String?,
    val incomplete: Boolean,
    val noWinStats: Boolean,
    val comments: String?,
    val syncTimestamp: Long,
    val initialPlayerCount: Int?,
    val syncHashCode: Int?,
    val dirtyTimestamp: Long?,
    val updateTimestamp: Long?,
    val deleteTimestamp: Long?,
    val startTime: Long?,
    val players: List<PlayPlayerLocal>?,
) {
    fun generateSyncHashCode(): Int {
        val sb = StringBuilder()
        sb.append(date).append("\n")
        sb.append(quantity).append("\n")
        sb.append(length).append("\n")
        sb.append(incomplete).append("\n")
        sb.append(noWinStats).append("\n")
        sb.append(location).append("\n")
        sb.append(comments).append("\n")
        players?.forEach { player ->
            sb.append(player.username).append("\n")
            sb.append(player.userId).append("\n")
            sb.append(player.name).append("\n")
            sb.append(player.startingPosition).append("\n")
            sb.append(player.color).append("\n")
            sb.append(player.score).append("\n")
            sb.append(player.isNew).append("\n")
            sb.append(player.rating).append("\n")
            sb.append(player.isWin).append("\n")
        }
        return sb.toString().hashCode()
    }
}
