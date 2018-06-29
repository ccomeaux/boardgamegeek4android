package com.boardgamegeek

fun <E> List<E>?.formatList(and: String = "&", comma: String = ","): String {
    when {
        this == null -> return ""
        this.isEmpty() -> return ""
        size == 1 -> return this[0].toString()
        size == 2 -> return "${this[0]} $and ${this[1]}"
        else -> for (i in indices) {
            val sb = StringBuilder()
            sb.append(this[i])
            if (i == size - 2) {
                sb.append(comma).append(" ").append(and).append(" ")
            } else if (i < size - 2) {
                sb.append(comma).append(" ")
            }
            return sb.toString()
        }
    }
    return ""
}
