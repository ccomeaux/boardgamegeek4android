package com.boardgamegeek.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.boardgamegeek.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateTimeUtils {
	public static final long UNPARSED_DATE = -2;
	public static final long UNKNOWN_DATE = -1;
	private static final DateFormat FORMAT_API = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private static final String FORMAT_MINUTES = "%d:%02d";

	private DateTimeUtils() {
	}

	/**
	 * Format minutes as "H hrs M mins" or "M mins" if less than an hour.
	 */
	public static String describeMinutes(Context context, int minutes) {
		Resources r = context.getResources();
		if (minutes < 60) {
			return String.valueOf(minutes) + " " + r.getString(R.string.minutes_abbr);
		} else {
			int hours = minutes / 60;
			int mins = minutes % 60;
			if (mins == 0) {
				return String.valueOf(hours) + " " + r.getString(R.string.hours_abbr);
			}
			return String.valueOf(hours) + " " + r.getString(R.string.hours_abbr) + " " +
				String.valueOf(mins) + " " + r.getString(R.string.minutes_abbr);
		}
	}

	/**
	 * Format minutes as "H:MM"
	 */
	public static String formatMinutes(int time) {
		if (time > 0) {
			int hours = time / 60;
			int minutes = time % 60;
			return String.format(FORMAT_MINUTES, hours, minutes);
		}
		return "0:00";
	}

	/**
	 * Determines how many days ago time was (rounded down).
	 */
	public static int howManyDaysOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.DAY_IN_MILLIS);
	}

	/**
	 * Determines how many days ago time was (rounded down).
	 */
	public static int howManyHoursOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.HOUR_IN_MILLIS);
	}

	/**
	 * Determines how many minutes ago time was (rounded up/down).
	 */
	public static int howManyMinutesOld(long time) {
		return (int) ((System.currentTimeMillis() - time + 30000) / DateUtils.MINUTE_IN_MILLIS);
	}

	/**
	 * Returns the long representation of the time "hours" ago.
	 */
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

	/**
	 * Formats a date for use in the API (<code>yyyy-mm-dd</code>)
	 */
	public static String formatDateForApi(long date) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(date);
		return FORMAT_API.format(c.getTime());
	}

	/**
	 * Formats the date for display in the forums (based on the users selected preference.
	 */
	public static CharSequence formatForumDate(Context context, long date) {
		if (PreferencesUtils.getForumDates(context)) {
			return DateUtils.formatDateTime(context, date,
				DateUtils.FORMAT_SHOW_DATE |
					DateUtils.FORMAT_SHOW_YEAR |
					DateUtils.FORMAT_ABBREV_MONTH |
					DateUtils.FORMAT_SHOW_TIME);
		} else {
			return PresentationUtils.describePastTimeSpan(date, context.getString(R.string.text_unknown));
		}
	}

	/**
	 * Attempt to parse the string date according to the given format. Will use the time parameter if it was already
	 * parsed.
	 */
	public static long tryParseDate(long time, String date, DateFormat format) {
		if (TextUtils.isEmpty(date)) {
			time = UNKNOWN_DATE;
		} else {
			if (time == UNPARSED_DATE) {
				try {
					time = format.parse(date).getTime();
				} catch (ParseException e) {
					time = UNKNOWN_DATE;
				}
			}
		}
		return time;
	}
}
