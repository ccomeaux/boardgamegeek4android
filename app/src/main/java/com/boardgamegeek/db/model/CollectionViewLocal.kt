package com.boardgamegeek.db.model

data class CollectionViewLocal(
    val id: Int,
    val name: String?,
    val sortType: Int?,
    val selectedCount: Int?,
    val selectedTimestamp: Long?,
    val starred: Boolean?,
    val filters: List<CollectionViewFilterLocal>? = null, // ignore
)