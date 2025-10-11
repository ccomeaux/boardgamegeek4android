package com.boardgamegeek.model

data class Location(
    val name: String,
    val playCount: Int
) {
    enum class SortType {
        Name, PlayCount
    }

    companion object {
        fun List<Location>.applySort(sortBy: SortType): List<Location> {
            return sortedWith(
                when (sortBy) {
                    SortType.Name -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.PlayCount -> compareByDescending<Location> { it.playCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }
}
