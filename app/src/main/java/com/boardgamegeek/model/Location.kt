package com.boardgamegeek.model

data class Location(
    val name: String,
    val playCount: Int
) {
    enum class SortType {
        NAME, PLAY_COUNT
    }

    companion object {
        fun List<Location>.applySort(sortBy: SortType): List<Location> {
            return sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.PLAY_COUNT -> compareByDescending<Location> { it.playCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }
}
