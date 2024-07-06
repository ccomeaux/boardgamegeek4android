package com.boardgamegeek.model

import com.boardgamegeek.extensions.asColorRgb

data class PlayerColor(
    val description: String,
    var sortOrder: Int = 0
) {
    val rgb = description.asColorRgb()
}
