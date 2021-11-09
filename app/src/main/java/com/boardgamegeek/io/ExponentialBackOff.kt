package com.boardgamegeek.io

import kotlin.random.Random

class ExponentialBackOff(
    private var initialIntervalMillis: Int = 500,
    private var randomizationFactor: Double = 0.5,
    private var multiplier: Double = 1.5,
    private var maxIntervalMillis: Int = 60_000,
    private var maxElapsedTimeMillis: Int = 900_000
) : BackOff {
    private var currentIntervalMillis: Int = 0
    private var startTimeNanos: Long = 0

    init {
        if (initialIntervalMillis <= 0) initialIntervalMillis = 500
        if (randomizationFactor < 0 || randomizationFactor >= 1) randomizationFactor = 0.5
        if (multiplier < 1) multiplier = 1.5
        if (maxIntervalMillis < initialIntervalMillis) maxIntervalMillis = initialIntervalMillis
        if (maxElapsedTimeMillis <= 0) maxElapsedTimeMillis = 900000
        reset()
    }

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
