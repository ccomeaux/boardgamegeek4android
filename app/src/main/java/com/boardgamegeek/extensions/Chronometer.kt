package com.boardgamegeek.extensions

import android.os.SystemClock
import android.widget.Chronometer

fun Chronometer.startTimerWithSystemTime(time: Long) {
    base = time - System.currentTimeMillis() + SystemClock.elapsedRealtime()
    start()
}