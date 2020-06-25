package com.boardgamegeek.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;

import com.boardgamegeek.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateTimeUtils {
	public static final long UNKNOWN_DATE = -1;
	public static final DateFormat FORMAT_API = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	public static final DateFormat FORMAT_DATABASE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	private DateTimeUtils() {
	}

	/**
	 * Format minutes as "H hrs M mins" or "M mins" if less than an hour.
	 */
	public static String describeMinutes(Context context, int minutes) {
		Resources r = context.getResources();
		if (minutes < 60) {
			return minutes + " " + r.getString(R.string.minutes_abbr);
		} else {
			int hours = minutes / 60;
			int mins = minutes % 60;
			if (mins == 0) {
				return hours + " " + r.getString(R.string.hours_abbr);
			}
			return hours + " " + r.getString(R.string.hours_abbr) + " " +
				mins + " " + r.getString(R.string.minutes_abbr);
		}
	}

	public static int howManyWeeksOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.WEEK_IN_MILLIS);
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

	public static String formatDateForDatabase(long date) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(date);
		return FORMAT_DATABASE.format(c.getTime());
	}
}
