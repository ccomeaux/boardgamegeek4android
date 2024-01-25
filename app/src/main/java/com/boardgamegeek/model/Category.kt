package com.boardgamegeek.model

data class Category(
    val id: Int,
    val name: String,
    val itemCount: Int = 0,
) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    companion object {
        fun List<Category>.applySort(sortBy: SortType): List<Category> {
            return sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ITEM_COUNT -> compareByDescending<Category> { it.itemCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }
}
