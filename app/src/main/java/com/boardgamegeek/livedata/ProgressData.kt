package com.boardgamegeek.livedata

data class ProgressData(
    val current: Int = 0,
    val max: Int = 0,
    val mode: Mode = Mode.OFF
) {
    enum class Mode {
        OFF, INDETERMINATE, DETERMINATE
    }
}
