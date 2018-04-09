package com.boardgamegeek.io

class FixedBackOff(private val intervalMillis: Int = 5000, private val maxBackOffCount: Int = 1) : BackOff {

    private var backOffCount: Int = 0

    init {
        checkArgument(intervalMillis > 0)
        checkArgument(maxBackOffCount >= 0)
        reset()
    }

    override fun nextBackOffMillis(): Long {
        backOffCount++
        return if (backOffCount > maxBackOffCount) BackOff.STOP else intervalMillis.toLong()
    }

    override fun reset() {
        backOffCount = 0
    }

    private fun checkArgument(expression: Boolean) {
        if (!expression) {
            throw IllegalArgumentException()
        }
    }
}
