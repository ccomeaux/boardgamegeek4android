package com.boardgamegeek.entities

data class CollectionViewShortcutEntity(
    val viewId: Long,
    val name: String,
    val count: Int,
    val timestamp: Long,
)
