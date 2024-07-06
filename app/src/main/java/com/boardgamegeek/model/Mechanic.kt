package com.boardgamegeek.model

data class Mechanic(
    val id: Int,
    val name: String,
    val itemCount: Int = 0,
) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    companion object {
        fun List<Mechanic>.applySort(sortBy: SortType): List<Mechanic> {
            return sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ITEM_COUNT -> compareByDescending<Mechanic> { it.itemCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }
}
