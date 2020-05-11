package com.boardgamegeek.extensions

import android.content.Context
import android.text.format.DateUtils.*
import androidx.annotation.StringRes
import com.boardgamegeek.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun Long.isOlderThan(duration: Int, timeUnit: TimeUnit) = System.currentTimeMillis() - this > timeUnit.toMillis(duration.toLong())

fun Long.asPastDaySpan(context: Context, @StringRes zeroResId: Int = R.string.never, includeWeekDay: Boolean = false): CharSequence {
    return if (this == 0L)
        context.getString(zeroResId)
    else {
        var flags = FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR or FORMAT_ABBREV_MONTH
        if (includeWeekDay) flags = flags or FORMAT_SHOW_WEEKDAY
        getRelativeTimeSpanString(this, System.currentTimeMillis(), DAY_IN_MILLIS, flags)
    }
}

fun Long.asPastMinuteSpan(context: Context): CharSequence {
    return if (this == 0L) context.getString(R.string.never) else getRelativeTimeSpanString(this, System.currentTimeMillis(), MINUTE_IN_MILLIS)
}

fun Long.formatTimestamp(context: Context, isForumTimestamp: Boolean, includeTime: Boolean): CharSequence {
    var flags = FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR or FORMAT_ABBREV_MONTH
    if (includeTime) flags = flags or FORMAT_SHOW_TIME
    val prefs = context.preferences()
    return if (isForumTimestamp && prefs[KEY_ADVANCED_DATES, false] == true) {
        formatDateTime(context, this, flags)
    } else {
        if (this == 0L) {
            context.getString(R.string.text_unknown)
        } else getRelativeTimeSpanString(this, System.currentTimeMillis(), MINUTE_IN_MILLIS, flags)
    }
}

val FORMAT_API: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun Long.asDateForApi(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = this
    return FORMAT_API.format(c.time)
}
