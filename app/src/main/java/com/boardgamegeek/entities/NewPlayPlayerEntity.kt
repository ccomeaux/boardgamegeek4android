package com.boardgamegeek.entities

data class NewPlayPlayerEntity(
        val name: String,
        val username: String,
        private val rawAvatarUrl: String = "") {
    constructor(player: PlayerEntity) : this(player.name, player.username, player.rawAvatarUrl)

    val id: String
        get() = if (username.isBlank()) name else username

    val avatarUrl: String = rawAvatarUrl
        get() = if (field == "N/A") "" else field

    val description: String = if (username.isBlank()) name else "$name ($username)"

    var color: String = ""

    var favoriteColors = emptyList<String>()
}
