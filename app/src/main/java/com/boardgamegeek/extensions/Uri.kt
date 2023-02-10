package com.boardgamegeek.extensions

import android.net.Uri
import com.boardgamegeek.provider.BggContract

fun Uri.getPathValue(path: String): String {
    val index = pathSegments.indexOf(path)
    return if (index == -1)
        ""
    else
        pathSegments.getOrNull(pathSegments.indexOf(path) + 1).orEmpty()
}

fun Uri.getPathValueAsInt(path: String): Int {
    val index = pathSegments.indexOf(path)
    return if (index == -1)
        BggContract.INVALID_ID
    else
        pathSegments.getOrNull(index + 1)?.toIntOrNull() ?: BggContract.INVALID_ID
}

fun Uri.getPathValueAsLong(path: String): Long {
    val index = pathSegments.indexOf(path)
    return if (index == -1)
        BggContract.INVALID_ID.toLong()
    else
        pathSegments.getOrNull(pathSegments.indexOf(path) + 1)?.toLongOrNull() ?: BggContract.INVALID_ID.toLong()
}
