package com.boardgamegeek.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GIndex(val g: Int = 0) : Comparable<GIndex> {
    init {
        require(g >= 0)
    }

    val description: String
        get() = if (isValid()) g.toString() else "?"

    fun isValid() = g > 0

    override fun compareTo(other: GIndex): Int {
        if (g == other.g) return 0
        if (g > other.g) return 1
        return -1
    }

    companion object {
        fun invalid(): GIndex {
            return GIndex(0)
        }

        suspend fun fromList(list: List<Int>): GIndex = withContext(Dispatchers.Default) {
            val counts = list.filter { it > 0 }.sortedDescending()

            var g = 0
            var gTotal = 0
            while (counts.size > g && (gTotal + counts[g] >= (g + 1) * (g + 1))) {
                gTotal += counts[g]
                g++
            }

            // TODO calculate rational g-index (but I don't know how to do it)

            GIndex(g)
        }
    }
}
