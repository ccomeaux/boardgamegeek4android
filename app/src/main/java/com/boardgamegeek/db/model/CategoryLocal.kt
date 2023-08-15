package com.boardgamegeek.db.model

class CategoryLocal(
    val internalId: Int,
    val id: Int,
    val name: String,
    val itemCount: Int = 0, // ignore
)
