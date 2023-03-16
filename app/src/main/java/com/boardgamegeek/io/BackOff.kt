package com.boardgamegeek.io

interface BackOff {
    fun nextBackOffMillis(): Long
    fun reset()

    companion object {
        const val STOP = -1L
    }
}
