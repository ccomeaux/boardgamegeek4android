package com.boardgamegeek.model

data class SearchResult(
    val id: Int,
    val name: String,
    val yearPublished: Int,
    val nameType: NameType = NameType.Unknown,
) {
    enum class NameType {
        Primary,
        Alternate,
        Unknown,
    }
}
