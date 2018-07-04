package com.boardgamegeek.extensions

import android.content.Context
import android.text.format.DateUtils
import com.boardgamegeek.R
import java.util.concurrent.TimeUnit

fun Long.isOlderThan(duration: Int, timeUnit: TimeUnit) = System.currentTimeMillis() - this > timeUnit.toMillis(duration.toLong())

fun Long.asPastDaySpan(context: Context): CharSequence {
    return if (this == 0L) context.getString(R.string.never) else DateUtils.getRelativeTimeSpanString(this, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS)
}

fun Long.asPastMinuteSpan(context: Context): CharSequence {
    return if (this == 0L) context.getString(R.string.never) else DateUtils.getRelativeTimeSpanString(this, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
}
