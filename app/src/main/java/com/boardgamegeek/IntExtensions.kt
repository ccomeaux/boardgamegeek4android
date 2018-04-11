package com.boardgamegeek

fun Int.clamp(min: Int, max: Int) = Math.max(min, Math.min(max, this))