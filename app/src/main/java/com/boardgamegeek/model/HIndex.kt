package com.boardgamegeek.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HIndex(val h: Int, val n: Int) : Comparable<HIndex> {
    init {
        require(h >= 0)
        require(n >= 0)
    }

    private val rational: Double
        get() = (if (isValid()) h + 1 - n.toDouble() / (2 * h + 1) else 0.0)

    val description: String
        get() = (if (isValid()) String.format("%.2f", rational) else "?")

    fun isValid() = (h > 0 || n > 0)

    override fun compareTo(other: HIndex): Int {
        if (h == other.h && n == other.n) return 0
        if (h > other.h) return 1
        if (n < other.n) return 1
        return -1
    }

    companion object {
        fun invalid(): HIndex {
            return HIndex(0, 0)
        }

        suspend fun fromList(list: List<Int>): HIndex = withContext(Dispatchers.Default) {
            val counts = list.filter { it > 0 }.sortedDescending()

            var hIndexCounter = 0
            var h = 0
            for (value in counts) {
                hIndexCounter++
                if (hIndexCounter > value) {
                    h = hIndexCounter - 1
                    break
                }
            }
            if (h == 0) h = hIndexCounter

            val nextH = h + 1
            var n = 0
            for ((index, value) in counts.withIndex()) {
                if (nextH > value) {
                    n += nextH - value
                }
                if (value < h) break
                if (index == h) break
            }
            if (counts.size == h) n += nextH

            HIndex(h, n)
        }
    }
}
