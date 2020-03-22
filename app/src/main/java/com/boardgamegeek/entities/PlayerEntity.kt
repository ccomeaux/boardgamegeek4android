package com.boardgamegeek.entities

data class PlayerEntity(
        val name: String,
        val username: String,
        val playCount: Int = 0,
        val winCount: Int = 0,
        val avatarUrl: String? = null) {
    val id: String
        get() {
            return if (username.isBlank()) name else username
        }
    val description: String = if (username.isBlank()) name else "$name ($username)"

    override fun hashCode(): Int {
        var hash = 3
        hash = 53 * hash + name.hashCode()
        hash = 53 * hash + username.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        val otherPlayerEntity = other as? PlayerEntity
        return when {
            otherPlayerEntity == null -> return false
            username.isBlank() -> {
                otherPlayerEntity.username.isBlank() && name == otherPlayerEntity.name
            }
            else -> username == otherPlayerEntity.username
        }
    }
}
