package com.boardgamegeek.entities

data class HIndexEntity(val h: Int, val n: Int) {
    private val rational: Double
        get() = if (h == INVALID_H_INDEX) 0.0 else h + 1 - n.toDouble() / (2 * h + 1)

    val description: String
        get() = if (h == INVALID_H_INDEX) "?" else String.format("%.2f", rational)

    companion object {
        const val INVALID_H_INDEX = -1

        fun fromList(list: List<Int>): HIndexEntity {
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

            return HIndexEntity(h, n)
        }
    }
}
