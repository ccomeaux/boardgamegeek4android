package com.boardgamegeek.util;

import java.text.DateFormat;
import java.text.ParseException;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;

import com.boardgamegeek.R;

public class DateTimeUtils {
	public static final long UNPARSED_DATE = -2;
	private static final long UNKNOWN_DATE = -1;

	private DateTimeUtils() {
	}

	public static String describeMinutes(Context context, int minutes) {
		Resources r = context.getResources();
		if (minutes < 60) {
			return String.valueOf(minutes) + " " + r.getString(R.string.minutes_abbr);
		} else {
			int hours = minutes / 60;
			int mins = minutes % 60;
			return String.valueOf(hours) + " " + r.getString(R.string.hours_abbr) + " " + String.valueOf(mins) + " "
				+ r.getString(R.string.minutes_abbr);
		}
	}

	public static int howManyDaysOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.DAY_IN_MILLIS);
	}

	public static int howManyHoursOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.HOUR_IN_MILLIS);
	}

	public static int howManyMinutesOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.MINUTE_IN_MILLIS);
	}

	public static long hoursAgo(int hours) {
		long timeInMillis = hours * DateUtils.HOUR_IN_MILLIS;
		return System.currentTimeMillis() - timeInMillis;
	}

	/**
	 * Formats a date for use in the API (<code>yyyy-mm-dd</code>)
	 */
	public static String formatDateForApi(int year, int month, int day) {
		return String.format("%04d", year) + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", day);
	}

	public static CharSequence formatForumDate(Context context, long date) {
		if (PreferencesUtils.getForumDates(context)) {
			return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
				| DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_TIME);
		} else {
			return DateUtils.getRelativeTimeSpanString(date);
		}
	}

	public static long tryParseDate(long time, String date, DateFormat format) {
		if (time == UNPARSED_DATE) {
			try {
				time = format.parse(date).getTime();
			} catch (ParseException e) {
				time = UNKNOWN_DATE;
			}
		}
		return time;
	}
}
