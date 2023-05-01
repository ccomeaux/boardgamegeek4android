package com.boardgamegeek.filterer

enum class TextOperator(val key: Int) {
    Is(1),
    IsNot(2),
    Contains(3),
    DoesNotContain(4),
    StartsWith(5),
    EndsWith(6),
    IsEmpty(7),
    IsNotEmpty(8);

    companion object {
        private val VALUES = values()
        fun getByKey(value: Int) = VALUES.firstOrNull { it.key == value }
        val default = Contains
    }
}