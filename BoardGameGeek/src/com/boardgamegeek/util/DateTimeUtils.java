package com.boardgamegeek.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.text.format.DateUtils;

public class DateTimeUtils {

	private static final String BGG_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final DateFormat FORMATER = new SimpleDateFormat(BGG_DATE_FORMAT, Locale.US);

	private DateTimeUtils() {
	}

	public static int howManyDaysOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.DAY_IN_MILLIS);
	}

	public static int howManyHoursOld(long time) {
		return (int) ((System.currentTimeMillis() - time) / DateUtils.HOUR_IN_MILLIS);
	}

	public static long parseDate(String inDate) {
		// TODO: need to apply time zone to get the right date
		inDate = removeTimeZone(inDate);

		try {
			final Date date = FORMATER.parse(inDate);
			return date.getTime();
		} catch (ParseException e) {
			return 0;
		}

	}

	private static String removeTimeZone(String inDate) {
		int index = inDate.lastIndexOf("-");
		if (index > 0) {
			inDate = inDate.substring(0, index);
		}
		return inDate;
	}
}
