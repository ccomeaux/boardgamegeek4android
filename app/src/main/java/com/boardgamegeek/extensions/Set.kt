package com.boardgamegeek.extensions

import androidx.compose.runtime.snapshots.SnapshotStateSet

fun <T> SnapshotStateSet<T>.addOrRemove(key: T) {
    if (contains(key))
        remove(key)
    else
        add(key)
}
