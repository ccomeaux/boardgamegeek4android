package com.boardgamegeek.repository

import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

class StatsHelper {
    companion object {
        fun calculateCorrelationCoefficient(listOfPairs: List<Pair<Double, Double>>): Double {
            if (listOfPairs.size < 2) return 0.0

            val meanX = listOfPairs.map { it.first }.average()
            val meanSquaredX = listOfPairs.map { it.first.pow(2) }.average()
            val dx = sqrt(meanSquaredX - meanX * meanX)

            val meanY = listOfPairs.map { it.second }.average()
            val meanSquaredY = listOfPairs.map { it.second.pow(2) }.average()
            val dy = sqrt(meanSquaredY - meanY * meanY)

            val d = dx * dy
            return if (d == 0.0)
                0.0
            else {
                val meanXY = listOfPairs.map { it.first * it.second }.average()
                ((meanXY - meanX * meanY) / d).coerceIn(-1.0, 1.0)
            }.also { c ->
                Timber.d("Correlation Coefficient=$c")
            }
        }
    }
}
