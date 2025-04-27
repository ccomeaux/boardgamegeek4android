package com.boardgamegeek.extensions

import androidx.core.view.isVisible
import com.google.android.material.progressindicator.LinearProgressIndicator

fun LinearProgressIndicator.setProgressOrIndeterminate(progress: Int, max: Int) {
    this.isVisible = true
    if (max > 0) {
        this.setProgressCompat(progress, true)
        this.max = max
        this.isIndeterminate = false
    } else {
        this.isIndeterminate = true
    }
}
