package com.boardgamegeek.model

import java.util.Date

data class Company(
    val internalId: Long,
    val id: Int,
    val name: String,
    val sortName: String,
    val description: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val heroImageUrl: String = "",
    val updatedTimestamp: Date?,
    val itemCount: Int = 0,
    val whitmoreScore: Int = 0,
    val statsUpdatedTimestamp: Date? = null,
) {
    override fun toString(): String {
        return "$name [$id]"
    }

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    companion object {
        fun List<Company>.applySort(sortBy: SortType): List<Company> {
            return sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.WHITMORE_SCORE -> compareByDescending<Company> { it.whitmoreScore }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ITEM_COUNT -> compareByDescending<Company> { it.itemCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }
}
