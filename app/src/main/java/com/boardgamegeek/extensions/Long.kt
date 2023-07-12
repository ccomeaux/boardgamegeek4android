package com.boardgamegeek.extensions

import android.content.Context
import android.text.format.DateUtils.*
import androidx.annotation.StringRes
import com.boardgamegeek.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration

fun Long.isOlderThan(duration: Duration) = System.currentTimeMillis() - this > duration.inWholeMilliseconds

fun Long.isToday(): Boolean = isToday(this)

fun Long.isSameDay(date: Long): Boolean {
    val otherCalendar = Calendar.getInstance()
    val cal2 = Calendar.getInstance()
    otherCalendar.timeInMillis = this
    cal2.timeInMillis = date
    return otherCalendar[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR] &&
            otherCalendar[Calendar.YEAR] == cal2[Calendar.YEAR]
}

fun Long.asPastDaySpan(context: Context, @StringRes zeroResId: Int = R.string.never, includeWeekDay: Boolean = false): CharSequence {
    return if (this == 0L)
        if (zeroResId != 0) context.getString(zeroResId) else ""
    else {
        var flags = FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR or FORMAT_ABBREV_ALL
        if (includeWeekDay) flags = flags or FORMAT_SHOW_WEEKDAY
        getRelativeTimeSpanString(this, System.currentTimeMillis(), DAY_IN_MILLIS, flags)
    }
}

fun Long.formatDateTime(context: Context?, @StringRes zeroResId: Int = R.string.never, flags: Int = 0): CharSequence {
    return if (this == 0L)
        if (zeroResId != 0) context?.getString(zeroResId) ?: "" else ""
    else {
        formatDateTime(context, this, flags)
    }
}

fun Long.howManyMinutesOld(): Int {
    return ((System.currentTimeMillis() - this + 30_000) / MINUTE_IN_MILLIS).toInt()
}

fun Long.howManyHoursOld(): Int {
    return ((System.currentTimeMillis() - this) / HOUR_IN_MILLIS).toInt()
}

fun Long.howManyWeeksOld(): Int {
    return ((System.currentTimeMillis() - this) / WEEK_IN_MILLIS).toInt()
}

fun Long.forDatabase(): String {
    val c = Calendar.getInstance()
    c.timeInMillis = this
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
}

fun Long.formatTimestamp(context: Context, includeTime: Boolean, isForumTimestamp: Boolean = false): CharSequence {
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

fun Long?.asDateForApi(): String {
    if (this == null) return ""
    if (this == 0L) return ""
    val c = Calendar.getInstance()
    c.timeInMillis = this
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
}

fun Long.fromLocalToUtc(): Long {
    val timeZone = TimeZone.getDefault()
    val standardTime = this - timeZone.rawOffset
    return standardTime - if (timeZone.inDaylightTime(Date(standardTime))) timeZone.dstSavings else 0
}
