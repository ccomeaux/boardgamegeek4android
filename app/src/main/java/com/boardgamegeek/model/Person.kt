package com.boardgamegeek.model

import java.util.Date

data class Person(
    val internalId: Long,
    val id: Int,
    val name: String,
    val description: String,
    val updatedTimestamp: Date?,
    val thumbnailUrl: String = "",
    val heroImageUrl: String = "",
    val imageUrl: String = "",
    val itemCount: Int = 0,
    val whitmoreScore: Int = 0,
    val statsUpdatedTimestamp: Date? = null,
    val imagesUpdatedTimestamp: Date? = null,
) {
    val heroImageUrls = listOf(heroImageUrl, thumbnailUrl, imageUrl)

    override fun toString(): String {
        return "$name [$id]"
    }

    enum class SortType {
        NAME, ITEM_COUNT, WHITMORE_SCORE
    }

    companion object {
        fun List<Person>.applySort(sortBy: SortType): List<Person> {
            return sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.WHITMORE_SCORE -> compareByDescending<Person> { it.whitmoreScore }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ITEM_COUNT -> compareByDescending<Person> { it.itemCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }
}
