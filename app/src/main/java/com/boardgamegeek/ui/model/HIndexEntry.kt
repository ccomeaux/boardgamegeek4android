package com.boardgamegeek.ui.model

class HIndexEntry(val playCount: Int, rank: Int, name: String) {
    val description = "$name (#$rank)"
}
