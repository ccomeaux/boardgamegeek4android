package com.boardgamegeek.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class HIndex(val h: Int, val n: Int) : Comparable<HIndex> {
    init {
        require(h >= 0)
        require(n >= 0)
    }

    private val rational: Double
        get() = (if (isValid()) h + 1 - n.toDouble() / (2 * h + 1) else 0.0)

    val description: String
        get() = (if (isValid()) String.format(Locale.getDefault(), "%.2f", rational) else "?")

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

            var h = 0
            while (counts.size > h && counts[h] > h) h++
            if (h == 0) h = counts.size

            val n = 2 * h + 1 - (counts.filter { it > h }.size) - (counts.getOrNull(h) ?: 0)

            HIndex(h, n)
        }
    }
}
