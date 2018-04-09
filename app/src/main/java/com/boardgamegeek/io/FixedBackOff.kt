package com.boardgamegeek.io

class FixedBackOff(private var intervalMillis: Int = 5000, private var maxBackOffCount: Int = 1) : BackOff {
    private var backOffCount: Int = 0

    init {
        if (intervalMillis <= 0) intervalMillis = 5000
        if (maxBackOffCount < 0) maxBackOffCount = 0
        reset()
    }

    override fun nextBackOffMillis(): Long {
        backOffCount++
        return if (backOffCount > maxBackOffCount) BackOff.STOP else intervalMillis.toLong()
    }

    override fun reset() {
        backOffCount = 0
    }
}
