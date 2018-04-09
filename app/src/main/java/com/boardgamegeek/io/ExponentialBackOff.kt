package com.boardgamegeek.io

class ExponentialBackOff(
        private val initialIntervalMillis: Int = 500,
        private val randomizationFactor: Double = 0.5,
        private val multiplier: Double = 1.5,
        private val maxIntervalMillis: Int = 60000,
        private val maxElapsedTimeMillis: Int = 900000) : BackOff {
    private var currentIntervalMillis: Int = 0
    private var startTimeNanos: Long = 0

    private val elapsedTimeMillis = (System.nanoTime() - startTimeNanos) / 1000000

    init {
        checkArgument(initialIntervalMillis > 0)
        checkArgument(0 <= randomizationFactor && randomizationFactor < 1)
        checkArgument(multiplier >= 1)
        checkArgument(maxIntervalMillis >= initialIntervalMillis)
        checkArgument(maxElapsedTimeMillis > 0)
        reset()
    }

    override fun reset() {
        currentIntervalMillis = initialIntervalMillis
        startTimeNanos = System.nanoTime()
    }

    override fun nextBackOffMillis(): Long {
        if (elapsedTimeMillis > maxElapsedTimeMillis) return BackOff.STOP
        val randomizedInterval = getRandomValueFromInterval(randomizationFactor, Math.random(), currentIntervalMillis)
        incrementCurrentInterval()
        return randomizedInterval.toLong()
    }

    private fun incrementCurrentInterval() {
        // Check for overflow, if overflow is detected set the current interval to the max interval.
        currentIntervalMillis = if (currentIntervalMillis >= maxIntervalMillis / multiplier) {
            maxIntervalMillis
        } else {
            (currentIntervalMillis * multiplier).toInt()
        }
    }

    companion object {
        fun getRandomValueFromInterval(randomizationFactor: Double, random: Double, currentIntervalMillis: Int): Int {
            val delta = randomizationFactor * currentIntervalMillis
            val minInterval = currentIntervalMillis - delta
            val maxInterval = currentIntervalMillis + delta
            return (minInterval + random * (maxInterval - minInterval + 1)).toInt()
        }

        private fun checkArgument(expression: Boolean) {
            if (!expression) {
                throw IllegalArgumentException()
            }
        }
    }
}
