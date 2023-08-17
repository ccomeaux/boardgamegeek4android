package com.boardgamegeek.db.model

data class MechanicLocal(
    val internalId: Int,
    val id: Int,
    val name: String,
    val itemCount: Int = 0, // ignore
)
