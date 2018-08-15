package com.boardgamegeek.mappers

import com.boardgamegeek.entities.SearchResultEntity
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.io.model.SearchResult

class SearchResultMapper {
    fun map(from: SearchResult): SearchResultEntity {
        return SearchResultEntity(
                id = from.id,
                name = from.name,
                yearPublished = getYearPublished(from.yearPublished),
                nameType = getNameType(from.nameType)
        )
    }

    private fun getYearPublished(yearPublished: String?): Int {
        if (yearPublished.isNullOrBlank()) return YEAR_UNKNOWN
        val l = yearPublished?.toLong() ?: YEAR_UNKNOWN.toLong()
        return if (l > Integer.MAX_VALUE) {
            try {
                (l - Long.MAX_VALUE).toInt() - 1
            } catch (e: Exception) {
                YEAR_UNKNOWN
            }
        } else {
            yearPublished?.toIntOrNull() ?: YEAR_UNKNOWN
        }
    }

    private fun getNameType(nameType: String): Int {
        return when (nameType) {
            "primary" -> SearchResultEntity.NAME_TYPE_PRIMARY
            "alternate" -> SearchResultEntity.NAME_TYPE_ALTERNATE
            else -> SearchResultEntity.NAME_TYPE_UNKNOWN
        }
    }
}
