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
        Name, ItemCount, WhitmoreScore
    }

    companion object {
        fun List<Person>.applySort(sortBy: SortType): List<Person> {
            return sortedWith(
                when (sortBy) {
                    SortType.Name -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.WhitmoreScore -> compareByDescending<Person> { it.whitmoreScore }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ItemCount -> compareByDescending<Person> { it.itemCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }
}
