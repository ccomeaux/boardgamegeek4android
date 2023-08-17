package com.boardgamegeek.db.model

data class CollectionViewFilterLocal(
    val id: Int,
    val viewId: Int,
    val type: Int?,
    val data: String?,
)
