package com.boardgamegeek.io

import kotlin.random.Random

class ExponentialBackOff(
    private val initialIntervalMillis: Int = 500, // the number of milliseconds between the initial request and first retry
    private val randomizationFactor: Double = 0.5, // the amount of jitter in each backoff timing. (1 = zero wait time up to double)
    private val multiplier: Double = 1.5, // amount to increase the interval on each subsequent attempt (1 is same as fixed backoff)
    private val maxIntervalMillis: Int = 60_000, // maximum number of milliseconds to wait on each retry
    private val maxElapsedTimeMillis: Int = 900_000, // total milliseconds in back-off attempt
) : BackOff {
    private var currentIntervalMillis: Int = initialIntervalMillis
    private var startTimeNanos: Long = System.nanoTime()

    override fun reset() {
        currentIntervalMillis = initialIntervalMillis
        startTimeNanos = System.nanoTime()
    }

    override fun nextBackOffMillis(): Long {
        if (calculateElapsedTimeMillis() > maxElapsedTimeMillis) return BackOff.STOP

        val delta = randomizationFactor * currentIntervalMillis
        val minInterval = currentIntervalMillis - delta
        val maxInterval = currentIntervalMillis + delta
        val randomizedInterval = (minInterval + Random.nextDouble() * (maxInterval - minInterval + 1)).toInt()

        incrementCurrentInterval()
        return randomizedInterval.toLong()
    }

    private fun calculateElapsedTimeMillis() = ((System.nanoTime() - startTimeNanos) / 1_000_000L).toInt()

    private fun incrementCurrentInterval() {
        // Check for overflow, if overflow is detected set the current interval to the max interval.
        currentIntervalMillis = if (currentIntervalMillis >= maxIntervalMillis / multiplier) {
            maxIntervalMillis
        } else {
            (currentIntervalMillis * multiplier).toInt()
        }
    }
}
