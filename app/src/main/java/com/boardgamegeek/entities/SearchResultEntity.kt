package com.boardgamegeek.entities

data class SearchResultEntity(
        val id: Int,
        val name: String,
        val yearPublished: Int,
        private val _nameType: String,
) {
    companion object {
        const val NAME_TYPE_PRIMARY = 0
        const val NAME_TYPE_ALTERNATE = 1
        const val NAME_TYPE_UNKNOWN = -1
    }

    val nameType
        get() = when (_nameType) {
            "primary" -> NAME_TYPE_PRIMARY
            "alternate" -> NAME_TYPE_ALTERNATE
            else -> NAME_TYPE_UNKNOWN
        }
}
