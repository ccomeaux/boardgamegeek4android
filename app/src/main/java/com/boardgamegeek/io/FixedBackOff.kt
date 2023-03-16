package com.boardgamegeek.io

class FixedBackOff(
    private val intervalMillis: Int = 5_000, // milliseconds between back-off attempts
    private val maxBackOffCount: Int = 1, // number of back-offs before quitting
) : BackOff {
    private var backOffCount: Int = 0

    override fun nextBackOffMillis(): Long {
        backOffCount++
        return if (backOffCount > maxBackOffCount) BackOff.STOP else intervalMillis.toLong()
    }

    override fun reset() {
        backOffCount = 0
    }
}
