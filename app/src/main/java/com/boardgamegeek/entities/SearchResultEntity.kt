package com.boardgamegeek.entities

data class SearchResultEntity(
        val id: Int,
        val name: String,
        val yearPublished: Int,
        val nameType: Int = NAME_TYPE_UNKNOWN
) {
    companion object {
        const val NAME_TYPE_PRIMARY = 0
        const val NAME_TYPE_ALTERNATE = 1
        const val NAME_TYPE_UNKNOWN = -1
    }
}
