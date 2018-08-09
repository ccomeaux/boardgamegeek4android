package com.boardgamegeek.extensions

fun <E> List<E>?.formatList(and: String = "&", comma: String = ","): String {
    when {
        this == null -> return ""
        this.isEmpty() -> return ""
        size == 1 -> return this[0].toString()
        size == 2 -> return "${this[0]} $and ${this[1]}"
        else -> {
            val sb = StringBuilder()
            for (i in indices) {
                sb.append(this[i])
                if (i == size - 2) {
                    sb.append(comma).append(" ").append(and).append(" ")
                } else if (i < size - 2) {
                    sb.append(comma).append(" ")
                }
            }
            return sb.toString()
        }
    }
}
