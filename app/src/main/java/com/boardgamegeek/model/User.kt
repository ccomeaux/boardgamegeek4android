package com.boardgamegeek.model

data class User(
    val username: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String,
    val playNickname: String = "",
    val updatedTimestamp: Long = 0L,
    val isBuddy: Boolean = false,
) {
    val fullName = "$firstName $lastName".trim()

    val nicknameCandidate = firstName.ifEmpty { lastName.ifEmpty { username } }

    override fun toString(): String {
        return if (username.isBlank()) fullName else "$fullName ($username)"
    }

    enum class SortType {
        FIRST_NAME, LAST_NAME, USERNAME
    }

    companion object {
        fun List<User>.applySort(sortBy: SortType): List<User> {
            return sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) {
                    when (sortBy) {
                        SortType.FIRST_NAME -> it.firstName
                        SortType.LAST_NAME -> it.lastName
                        SortType.USERNAME -> it.username
                    }
                }
            )
        }
    }
}
